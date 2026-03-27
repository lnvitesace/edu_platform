package com.edu.platform.controller;

import com.edu.platform.dto.AuthResponse;
import com.edu.platform.dto.LoginRequest;
import com.edu.platform.dto.RegisterRequest;
import com.edu.platform.dto.UserResponse;
import com.edu.platform.entity.User;
import com.edu.platform.exception.BadRequestException;
import com.edu.platform.security.UserPrincipal;
import com.edu.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test user
        User testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(User.UserRole.STUDENT)
                .status(User.UserStatus.ACTIVE)
                .build();

        testUserPrincipal = UserPrincipal.create(testUser);

        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .firstName("Test")
                .lastName("User")
                .role("STUDENT")
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("STUDENT")
                .status("ACTIVE")
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("access.token.jwt")
                .refreshToken("refresh.token.jwt")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userResponse)
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void register_Success() throws Exception {
        // Arrange
        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token.jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation fails with blank username")
    void register_BlankUsername_ValidationFails() throws Exception {
        // Arrange
        registerRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation fails with invalid email")
    void register_InvalidEmail_ValidationFails() throws Exception {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Rejects ADMIN role")
    void register_AdminRole_ReturnsBadRequest() throws Exception {
        registerRequest.setRole("ADMIN");
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Role must be STUDENT or INSTRUCTOR"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role must be STUDENT or INSTRUCTOR"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void login_Success() throws Exception {
        // Arrange
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token.jwt"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Validation fails with blank password")
    void login_BlankPassword_ValidationFails() throws Exception {
        // Arrange
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Success")
    void refreshToken_Success() throws Exception {
        // Arrange
        String refreshToken = "valid.refresh.token";
        when(userService.refreshToken(refreshToken)).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token.jwt"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Missing refreshToken returns 400")
    void refreshToken_MissingToken_ReturnsBadRequest() throws Exception {
        when(userService.refreshToken(null)).thenThrow(new BadRequestException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/logout - Success")
    void logout_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Unauthorized without authentication")
    void logout_Unauthorized() throws Exception {
        // Act & Assert - Should return 403 Forbidden or 500 due to null UserPrincipal
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
