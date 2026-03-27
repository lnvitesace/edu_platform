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
import com.edu.platform.security.JwtTokenProvider;
import com.edu.platform.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserProfile testUserProfile;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UpdateProfileRequest updateProfileRequest;
    private UserPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .firstName("Test")
                .lastName("User")
                .phone("+1234567890")
                .role(User.UserRole.STUDENT)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        testUserProfile = UserProfile.builder()
                .id(1L)
                .user(testUser)
                .bio("Test bio")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .country("USA")
                .city("New York")
                .build();

        testUser.setProfile(testUserProfile);

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123")
                .firstName("New")
                .lastName("User")
                .phone("+1234567890")
                .role("STUDENT")
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123")
                .build();

        updateProfileRequest = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .bio("Updated bio")
                .city("Los Angeles")
                .build();

        userPrincipal = UserPrincipal.create(testUser);

        authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities()
        );

        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");
    }

    // ============================================
    // REGISTER TESTS
    // ============================================

    @Test
    @DisplayName("Register - Success with all fields")
    void register_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        AuthResponse response = userService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider).generateAccessToken(authentication);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(redisTemplate.opsForValue()).set(
                eq("user:session:" + testUser.getId()),
                eq("access.token.jwt"),
                eq(86400000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Register - Fail when username already exists")
    void register_UsernameExists_ThrowsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Fail when email already exists")
    void register_EmailExists_ThrowsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already in use");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Creates user with default STUDENT role when role is null")
    void register_NullRole_DefaultsToStudent() {
        registerRequest.setRole(null);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        userService.register(registerRequest);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(User.UserRole.STUDENT);
    }

    @Test
    @DisplayName("Register - Password is encoded before saving")
    void register_PasswordEncoded() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedPassword");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        userService.register(registerRequest);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedPassword");
        verify(passwordEncoder).encode("Password123");
    }

    // ============================================
    // LOGIN TESTS
    // ============================================

    @Test
    @DisplayName("Login - Success with valid credentials")
    void login_Success() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        AuthResponse response = userService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());

        UsernamePasswordAuthenticationToken capturedAuth =
                (UsernamePasswordAuthenticationToken) authCaptor.getValue();
        assertThat(capturedAuth.getPrincipal()).isEqualTo("testuser");
        assertThat(capturedAuth.getCredentials()).isEqualTo("Password123");
    }

    @Test
    @DisplayName("Login - Updates last login time")
    void login_UpdatesLastLoginTime() {
        LocalDateTime beforeLogin = LocalDateTime.now().minusMinutes(1);
        testUser.setLastLoginAt(beforeLogin);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);

        userService.login(loginRequest);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getLastLoginAt()).isAfter(beforeLogin);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Login - Caches session in Redis")
    void login_CachesSessionInRedis() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        userService.login(loginRequest);

        verify(redisTemplate.opsForValue()).set(
                eq("user:session:1"),
                eq("access.token.jwt"),
                eq(86400000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Login - Throws exception when user not found after authentication")
    void login_UserNotFound_ThrowsException() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("id")
                .hasMessageContaining("1");

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(userRepository).findById(1L);
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Login - Propagates authentication failure")
    void login_AuthenticationFailure_ThrowsException() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findById(anyLong());
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    // ============================================
    // REFRESH TOKEN TESTS
    // ============================================

    @Test
    @DisplayName("RefreshToken - Success with valid token")
    void refreshToken_Success() {
        String refreshTokenString = "valid.refresh.token";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token(refreshTokenString)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString))
                .thenReturn(Optional.of(refreshToken));
        when(tokenProvider.generateAccessToken(any(Authentication.class)))
                .thenReturn("new.access.token");
        when(tokenProvider.generateRefreshToken(1L))
                .thenReturn("new.refresh.token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        AuthResponse response = userService.refreshToken(refreshTokenString);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");

        verify(refreshTokenRepository).delete(refreshToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(redisTemplate.opsForValue()).set(
                eq("user:session:1"),
                eq("new.access.token"),
                eq(86400000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("RefreshToken - Throws exception for invalid token")
    void refreshToken_InvalidToken_ThrowsException() {
        when(refreshTokenRepository.findByToken("invalid.token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refreshToken("invalid.token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid refresh token");

        verify(refreshTokenRepository).findByToken("invalid.token");
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("RefreshToken - Throws exception for expired token")
    void refreshToken_ExpiredToken_ThrowsException() {
        String refreshTokenString = "expired.refresh.token";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token(refreshTokenString)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> userService.refreshToken(refreshTokenString))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Refresh token has expired");

        verify(refreshTokenRepository).findByToken(refreshTokenString);
        verify(refreshTokenRepository).delete(refreshToken);
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("RefreshToken - Deletes old token and creates new one")
    void refreshToken_DeletesOldAndCreatesNew() {
        String oldTokenString = "old.refresh.token";
        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token(oldTokenString)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken(oldTokenString))
                .thenReturn(Optional.of(oldToken));
        when(tokenProvider.generateAccessToken(any(Authentication.class)))
                .thenReturn("new.access.token");
        when(tokenProvider.generateRefreshToken(1L))
                .thenReturn("new.refresh.token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        userService.refreshToken(oldTokenString);

        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).save(argThat(token ->
            token.getToken().equals("new.refresh.token") &&
            token.getUser().getId().equals(1L)
        ));
    }

    // ============================================
    // LOGOUT TESTS
    // ============================================

    @Test
    @DisplayName("Logout - Deletes refresh tokens and removes session")
    void logout_Success() {
        userService.logout(1L);

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(redisTemplate).delete("user:session:1");
        verify(redisTemplate).delete("user:details:1");
    }

    @Test
    @DisplayName("Logout - Handles null userId gracefully")
    void logout_NullUserId() {
        userService.logout(null);

        verify(refreshTokenRepository).deleteByUserId(null);
        verify(redisTemplate).delete("user:session:null");
        verify(redisTemplate).delete("user:details:null");
    }

    // ============================================
    // GET USER TESTS
    // ============================================

    @Test
    @DisplayName("GetUserById - Returns user successfully")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getProfile()).isNotNull();
        assertThat(response.getProfile().getBio()).isEqualTo("Test bio");

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("GetUserById - Throws exception when user not found")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("id")
                .hasMessageContaining("999");

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("GetCurrentUser - Returns current user successfully")
    void getCurrentUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getCurrentUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    // ============================================
    // UPDATE PROFILE TESTS
    // ============================================

    @Test
    @DisplayName("UpdateProfile - Updates all fields successfully")
    void updateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testUserProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        UserResponse response = userService.updateProfile(1L, updateProfileRequest);

        assertThat(response).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Name");

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile updatedProfile = profileCaptor.getValue();
        assertThat(updatedProfile.getBio()).isEqualTo("Updated bio");
        assertThat(updatedProfile.getCity()).isEqualTo("Los Angeles");

        verify(redisTemplate).delete("user:details:1");
    }

    @Test
    @DisplayName("UpdateProfile - Updates only provided fields")
    void updateProfile_PartialUpdate() {
        UpdateProfileRequest partialRequest = UpdateProfileRequest.builder()
                .firstName("OnlyFirstName")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testUserProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        userService.updateProfile(1L, partialRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();

        assertThat(updatedUser.getFirstName()).isEqualTo("OnlyFirstName");
        assertThat(updatedUser.getLastName()).isEqualTo("User");

        verify(redisTemplate).delete("user:details:1");
    }

    @Test
    @DisplayName("UpdateProfile - Throws exception when user not found")
    void updateProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(999L, updateProfileRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdateProfile - Throws exception when profile not found")
    void updateProfile_ProfileNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(1L, updateProfileRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UserProfile");

        verify(userProfileRepository).findByUserId(1L);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdateProfile - Updates all profile fields")
    void updateProfile_AllFields() {
        UpdateProfileRequest fullRequest = UpdateProfileRequest.builder()
                .firstName("New")
                .lastName("Name")
                .phone("+9876543210")
                .avatarUrl("https://example.com/avatar.jpg")
                .bio("New bio")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("Female")
                .country("Canada")
                .city("Toronto")
                .address("123 Main St")
                .postalCode("M5V 1A1")
                .educationLevel("Bachelor")
                .interests("Reading, Coding")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testUserProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        userService.updateProfile(1L, fullRequest);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile updatedProfile = profileCaptor.getValue();

        assertThat(updatedProfile.getBio()).isEqualTo("New bio");
        assertThat(updatedProfile.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
        assertThat(updatedProfile.getGender()).isEqualTo("Female");
        assertThat(updatedProfile.getCountry()).isEqualTo("Canada");
        assertThat(updatedProfile.getCity()).isEqualTo("Toronto");
        assertThat(updatedProfile.getAddress()).isEqualTo("123 Main St");
        assertThat(updatedProfile.getPostalCode()).isEqualTo("M5V 1A1");
        assertThat(updatedProfile.getEducationLevel()).isEqualTo("Bachelor");
        assertThat(updatedProfile.getInterests()).isEqualTo("Reading, Coding");

        verify(redisTemplate).delete("user:details:1");
    }

    // ============================================
    // EDGE CASES & ERROR SCENARIOS
    // ============================================

    @Test
    @DisplayName("MapToUserResponse - Handles null profile gracefully")
    void mapToUserResponse_NullProfile() {
        testUser.setProfile(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getProfile()).isNull();
    }

    @Test
    @DisplayName("Register - Creates user with INSTRUCTOR role")
    void register_InstructorRole() {
        registerRequest.setRole("INSTRUCTOR");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        User instructorUser = User.builder()
                .id(2L)
                .username("instructor")
                .email("instructor@example.com")
                .role(User.UserRole.INSTRUCTOR)
                .status(User.UserStatus.ACTIVE)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(instructorUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUserProfile);

        userService.register(registerRequest);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(User.UserRole.INSTRUCTOR);
    }

    @Test
    @DisplayName("Register - Rejects ADMIN role from public registration")
    void register_AdminRole_Rejected() {
        registerRequest.setRole("ADMIN");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Role must be STUDENT or INSTRUCTOR");

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Register - Rejects unknown role from public registration")
    void register_InvalidRole_Rejected() {
        registerRequest.setRole("MODERATOR");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Role must be STUDENT or INSTRUCTOR");

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("RefreshToken - Updates Redis cache with new token")
    void refreshToken_UpdatesRedisCache() {
        String refreshTokenString = "valid.refresh.token";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token(refreshTokenString)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString))
                .thenReturn(Optional.of(refreshToken));
        when(tokenProvider.generateAccessToken(any(Authentication.class)))
                .thenReturn("new.access.token");
        when(tokenProvider.generateRefreshToken(1L))
                .thenReturn("new.refresh.token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        userService.refreshToken(refreshTokenString);

        verify(redisTemplate.opsForValue()).set(
                eq("user:session:1"),
                eq("new.access.token"),
                eq(86400000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("CreateRefreshToken - Saves token with correct expiration")
    void createRefreshToken_CorrectExpiration() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token.jwt");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh.token.jwt");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(tokenCaptor.capture())).thenReturn(new RefreshToken());

        userService.login(loginRequest);

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("refresh.token.jwt");
        assertThat(savedToken.getUser().getId()).isEqualTo(1L);
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedToken.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
    }
}
