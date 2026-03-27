package com.edu.platform.controller;

import com.edu.platform.dto.UserResponse;
import com.edu.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("InternalApiController Tests")
class InternalApiControllerTest {

    private static final String INTERNAL_TOKEN = "test-internal-service-token";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/internal/users/{id} - Forbidden without internal token")
    void getUserById_WithoutInternalToken_Forbidden() throws Exception {
        mockMvc.perform(get("/api/internal/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/users/{id} - Forbidden with wrong internal token")
    void getUserById_WithWrongInternalToken_Forbidden() throws Exception {
        mockMvc.perform(get("/api/internal/users/1")
                        .header("X-Internal-Service-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/users/{id} - Success with internal token")
    void getUserById_WithInternalToken_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(UserResponse.builder()
                .id(1L)
                .username("internal-user")
                .email("internal@example.com")
                .role("STUDENT")
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/api/internal/users/1")
                        .header("X-Internal-Service-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("internal-user"));
    }
}
