package com.edu.platform.service;

import com.edu.platform.dto.AuthResponse;
import com.edu.platform.dto.LoginRequest;
import com.edu.platform.dto.RegisterRequest;
import com.edu.platform.entity.User;
import com.edu.platform.exception.BadRequestException;
import com.edu.platform.repository.RefreshTokenRepository;
import com.edu.platform.repository.UserProfileRepository;
import com.edu.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService Integration Tests")
@Import(com.edu.platform.config.InMemoryRedisTestConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        if (redisTemplate instanceof com.edu.platform.config.InMemoryRedisTestConfig.InMemoryStringRedisTemplate inMemory) {
            inMemory.clear();
        }

        registerRequest = RegisterRequest.builder()
                .username("integrationuser")
                .email("integration@example.com")
                .password("Password123")
                .firstName("Integration")
                .lastName("Test")
                .phone("+1234567890")
                .role("STUDENT")
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("integrationuser")
                .password("Password123")
                .build();
    }

    @Test
    @DisplayName("Complete Registration Flow")
    void testCompleteRegistrationFlow() {
        AuthResponse response = userService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("integrationuser");
        assertThat(response.getUser().getEmail()).isEqualTo("integration@example.com");

        User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getPassword()).isNotEqualTo("Password123");
        assertThat(savedUser.getPassword()).startsWith("$2a$");

        assertThat(userProfileRepository.findByUserId(savedUser.getId())).isPresent();
        assertThat(refreshTokenRepository.findByToken(response.getRefreshToken())).isPresent();

        String cacheKey = "user:session:" + savedUser.getId();
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
    }

    @Test
    @DisplayName("Complete Login Flow After Registration")
    void testCompleteLoginFlow() {
        userService.register(registerRequest);
        AuthResponse loginResponse = userService.login(loginRequest);

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotEmpty();
        assertThat(loginResponse.getRefreshToken()).isNotEmpty();
        assertThat(loginResponse.getUser().getUsername()).isEqualTo("integrationuser");

        User user = userRepository.findByUsername("integrationuser").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Login Fails with Wrong Password")
    void testLoginWithWrongPassword() {
        userService.register(registerRequest);
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .usernameOrEmail("integrationuser")
                .password("WrongPassword")
                .build();

        assertThatThrownBy(() -> userService.login(wrongPasswordRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Complete Token Refresh Flow")
    void testCompleteTokenRefreshFlow() {
        AuthResponse registerResponse = userService.register(registerRequest);
        String oldRefreshToken = registerResponse.getRefreshToken();
        String oldAccessToken = registerResponse.getAccessToken();

        AuthResponse refreshResponse = userService.refreshToken(oldRefreshToken);

        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotEmpty();
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(oldAccessToken);
        assertThat(refreshResponse.getRefreshToken()).isNotEmpty();
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(oldRefreshToken);

        assertThat(refreshTokenRepository.findByToken(oldRefreshToken)).isEmpty();
        assertThat(refreshTokenRepository.findByToken(refreshResponse.getRefreshToken())).isPresent();
    }

    @Test
    @DisplayName("Registration with Duplicate Username Fails")
    void testRegistrationWithDuplicateUsername() {
        userService.register(registerRequest);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username("integrationuser")
                .email("different@example.com")
                .password("Password123")
                .firstName("Different")
                .lastName("User")
                .build();

        assertThatThrownBy(() -> userService.register(duplicateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    @DisplayName("Registration with Duplicate Email Fails")
    void testRegistrationWithDuplicateEmail() {
        userService.register(registerRequest);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username("differentuser")
                .email("integration@example.com")
                .password("Password123")
                .firstName("Different")
                .lastName("User")
                .build();

        assertThatThrownBy(() -> userService.register(duplicateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    @DisplayName("Complete Logout Flow")
    void testCompleteLogoutFlow() {
        AuthResponse response = userService.register(registerRequest);
        Long userId = response.getUser().getId();
        String refreshToken = response.getRefreshToken();

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

        userService.logout(userId);

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();

        String cacheKey = "user:session:" + userId;
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }

    @Test
    @DisplayName("Login with Email Instead of Username")
    void testLoginWithEmail() {
        userService.register(registerRequest);

        LoginRequest emailLoginRequest = LoginRequest.builder()
                .usernameOrEmail("integration@example.com")
                .password("Password123")
                .build();

        AuthResponse response = userService.login(emailLoginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getUser().getEmail()).isEqualTo("integration@example.com");
    }

    @Test
    @DisplayName("Multiple Users Can Register and Login")
    void testMultipleUsers() {
        RegisterRequest user1 = RegisterRequest.builder()
                .username("user1")
                .email("user1@example.com")
                .password("Password123")
                .firstName("User")
                .lastName("One")
                .role("STUDENT")
                .build();

        RegisterRequest user2 = RegisterRequest.builder()
                .username("user2")
                .email("user2@example.com")
                .password("Password123")
                .firstName("User")
                .lastName("Two")
                .role("INSTRUCTOR")
                .build();

        AuthResponse response1 = userService.register(user1);
        AuthResponse response2 = userService.register(user2);

        assertThat(response1.getUser().getUsername()).isEqualTo("user1");
        assertThat(response1.getUser().getRole()).isEqualTo("STUDENT");

        assertThat(response2.getUser().getUsername()).isEqualTo("user2");
        assertThat(response2.getUser().getRole()).isEqualTo("INSTRUCTOR");

        LoginRequest login1 = LoginRequest.builder()
                .usernameOrEmail("user1")
                .password("Password123")
                .build();

        LoginRequest login2 = LoginRequest.builder()
                .usernameOrEmail("user2")
                .password("Password123")
                .build();

        AuthResponse loginResponse1 = userService.login(login1);
        AuthResponse loginResponse2 = userService.login(login2);

        assertThat(loginResponse1.getAccessToken()).isNotEmpty();
        assertThat(loginResponse2.getAccessToken()).isNotEmpty();
    }
}
