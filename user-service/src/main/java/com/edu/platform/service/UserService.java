package com.edu.platform.service;

import com.edu.platform.dto.*;
import com.edu.platform.entity.RefreshToken;
import com.edu.platform.entity.User;
import com.edu.platform.entity.UserProfile;
import com.edu.platform.exception.BadRequestException;
import com.edu.platform.exception.ResourceNotFoundException;
import com.edu.platform.repository.RefreshTokenRepository;
import com.edu.platform.repository.UserProfileRepository;
import com.edu.platform.repository.UserRepository;
import com.edu.platform.security.CachedUserDetails;
import com.edu.platform.security.JwtTokenProvider;
import com.edu.platform.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 用户服务 - 认证与用户管理的核心业务逻辑
 *
 * <h3>设计决策：</h3>
 * <ul>
 *   <li>Refresh Token 存 DB 而非 Redis：需要持久化，服务重启后 token 仍有效；
 *       且支持一用户多设备登录（多条 refresh token 记录）</li>
 *   <li>Session 存 Redis：高频访问（每次请求验证），内存读取性能优于 DB</li>
 *   <li>User Details 也缓存到 Redis：避免每次请求都查库加载用户信息</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String DETAILS_KEY_PREFIX = "user:details:";

    private static final String ERR_USERNAME_TAKEN = "Username is already taken";
    private static final String ERR_EMAIL_IN_USE = "Email is already in use";
    private static final String ERR_INVALID_REFRESH_TOKEN = "Invalid refresh token";
    private static final String ERR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    private static final String ERR_INVALID_ROLE = "Role must be STUDENT or INSTRUCTOR";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 用户注册
     *
     * <p>使用 @Transactional 因为涉及 users 和 user_profiles 两张表的写入，
     * 必须保证原子性：要么都成功，要么都回滚。</p>
     *
     * <p>注册成功后自动登录并返回 Token，避免用户再次输入凭证，提升体验。</p>
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(ERR_USERNAME_TAKEN);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(ERR_EMAIL_IN_USE);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(parseRole(request.getRole()))
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        // 创建空 profile，后续用户可补充；与 User 分表避免主表字段过多
        userProfileRepository.save(UserProfile.builder().user(user).build());

        Authentication auth = authenticate(request.getUsername(), request.getPassword());
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return buildAuthResponse(user, auth, principal);
    }

    /**
     * 用户登录
     *
     * <p>使用 @Transactional 虽然只更新 lastLoginAt 一个字段，
     * 但保持与其他写操作的一致性，且便于未来扩展（如登录日志）。</p>
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticate(request.getUsernameOrEmail(), request.getPassword());
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        User user = getUserOrThrow(principal.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user, auth, principal);
    }

    /**
     * 刷新 Token
     *
     * <p>采用 Rotation 策略：每次刷新都删除旧 token、生成新 token。
     * 相比固定 token，可检测 token 泄露（旧 token 被使用时会失败）。</p>
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException(ERR_INVALID_REFRESH_TOKEN));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException(ERR_REFRESH_TOKEN_EXPIRED);
        }

        User user = token.getUser();
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        refreshTokenRepository.delete(token);
        return buildAuthResponse(user, auth, principal);
    }

    /**
     * 用户登出
     *
     * <p>同时清理 DB（refresh token）和 Redis（session + details）。
     * 使用 @Transactional 保证 DB 操作的原子性；Redis 操作即使失败也不回滚 DB，
     * 因为 session 过期后自然失效，不影响安全性。</p>
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        redisTemplate.delete(SESSION_KEY_PREFIX + userId);
        redisTemplate.delete(DETAILS_KEY_PREFIX + userId);
    }

    /**
     * 使用 readOnly=true 优化只读事务：跳过脏检查、可使用只读数据库副本
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return mapToUserResponse(getUserOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return getUserById(userId);
    }

    /**
     * 更新用户资料
     *
     * <p>使用 @Transactional 因为涉及 users 和 user_profiles 两张表。
     * 采用部分更新策略（仅更新非空字段），避免客户端必须传递完整数据。</p>
     *
     * <p>更新后主动失效 Redis 缓存，采用 Cache-Aside 模式：
     * 写时删除缓存，读时回填。相比写时更新缓存，可避免并发写导致的缓存不一致。</p>
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);

        updateIfPresent(request.getFirstName(), user::setFirstName);
        updateIfPresent(request.getLastName(), user::setLastName);
        updateIfPresent(request.getPhone(), user::setPhone);
        updateIfPresent(request.getAvatarUrl(), user::setAvatarUrl);
        userRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));

        updateIfPresent(request.getBio(), profile::setBio);
        updateIfPresent(request.getDateOfBirth(), profile::setDateOfBirth);
        updateIfPresent(request.getGender(), profile::setGender);
        updateIfPresent(request.getCountry(), profile::setCountry);
        updateIfPresent(request.getCity(), profile::setCity);
        updateIfPresent(request.getAddress(), profile::setAddress);
        updateIfPresent(request.getPostalCode(), profile::setPostalCode);
        updateIfPresent(request.getEducationLevel(), profile::setEducationLevel);
        updateIfPresent(request.getInterests(), profile::setInterests);
        userProfileRepository.save(profile);

        redisTemplate.delete(DETAILS_KEY_PREFIX + userId);

        return mapToUserResponse(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
    }

    private AuthResponse buildAuthResponse(User user, Authentication auth, UserPrincipal principal) {
        String accessToken = tokenProvider.generateAccessToken(auth);
        String refreshToken = createRefreshToken(user);
        cacheUserSession(user.getId(), accessToken, principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    private String createRefreshToken(User user) {
        String token = tokenProvider.generateRefreshToken(user.getId());
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    /**
     * 缓存 session 和 user details 到 Redis
     *
     * <p>session 用于验证 token 是否被撤销（登出后失效）；
     * details 用于避免每次请求查库加载用户信息。
     * 两者 TTL 与 access token 一致，token 过期时缓存自动清理。</p>
     */
    private void cacheUserSession(Long userId, String token, UserPrincipal principal) {
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + userId, token,
                tokenProvider.getJwtExpirationMs(), TimeUnit.MILLISECONDS);
        try {
            String detailsJson = objectMapper.writeValueAsString(CachedUserDetails.from(principal));
            redisTemplate.opsForValue().set(
                    DETAILS_KEY_PREFIX + userId, detailsJson,
                    tokenProvider.getJwtExpirationMs(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // 序列化失败不影响主流程，details 缓存未命中时会从 DB 加载
        }
    }

    private User.UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return User.UserRole.STUDENT;
        }

        String normalizedRole = role.toUpperCase();
        if ("STUDENT".equals(normalizedRole)) {
            return User.UserRole.STUDENT;
        }
        if ("INSTRUCTOR".equals(normalizedRole)) {
            return User.UserRole.INSTRUCTOR;
        }

        throw new BadRequestException(ERR_INVALID_ROLE);
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        Optional.ofNullable(value).ifPresent(setter);
    }

    private UserResponse mapToUserResponse(User user) {
        UserProfileResponse profileResponse = Optional.ofNullable(user.getProfile())
                .map(this::mapToProfileResponse)
                .orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .profile(profileResponse)
                .build();
    }

    private UserProfileResponse mapToProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .bio(profile.getBio())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .country(profile.getCountry())
                .city(profile.getCity())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .educationLevel(profile.getEducationLevel())
                .interests(profile.getInterests())
                .build();
    }
}
