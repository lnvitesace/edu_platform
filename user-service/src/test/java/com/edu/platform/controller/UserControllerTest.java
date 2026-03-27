package com.edu.platform.controller;

import com.edu.platform.dto.UpdateProfileRequest;
import com.edu.platform.dto.UserResponse;
import com.edu.platform.entity.User;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private UserResponse userResponse;
    private UpdateProfileRequest updateProfileRequest;
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

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("STUDENT")
                .status("ACTIVE")
                .build();

        updateProfileRequest = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .bio("Updated bio")
                .build();
    }

    @Test
    @DisplayName("GET /api/users/me - Success with authentication")
    void getCurrentUser_Success() throws Exception {
        // Arrange
        when(userService.getCurrentUser(1L)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/me - Unauthorized without authentication")
    void getCurrentUser_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Success with authentication")
    void getUserById_Success() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Unauthorized without authentication")
    void getUserById_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/users/me - Success")
    void updateProfile_Success() throws Exception {
        // Arrange
        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Updated")
                .lastName("Name")
                .role("STUDENT")
                .status("ACTIVE")
                .build();

        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @DisplayName("PUT /api/users/me - Unauthorized without authentication")
    void updateProfile_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isForbidden());
    }
}
