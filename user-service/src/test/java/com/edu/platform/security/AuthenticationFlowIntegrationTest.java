package com.edu.platform.security;

import com.edu.platform.dto.AuthResponse;
import com.edu.platform.dto.LoginRequest;
import com.edu.platform.dto.RegisterRequest;
import com.edu.platform.repository.RefreshTokenRepository;
import com.edu.platform.repository.UserProfileRepository;
import com.edu.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Flow Integration Tests")
@Import(com.edu.platform.config.InMemoryRedisTestConfig.class)
class AuthenticationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        if (redisTemplate instanceof com.edu.platform.config.InMemoryRedisTestConfig.InMemoryStringRedisTemplate inMemory) {
            inMemory.clear();
        }
    }

    private RegisterRequest createUniqueRegisterRequest() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return RegisterRequest.builder()
                .username("testuser" + unique)
                .email("test" + unique + "@example.com")
                .password("Password123")
                .firstName("Test")
                .lastName("User")
                .phone("+1234567890")
                .role("STUDENT")
                .build();
    }

    private AuthResponse register(RegisterRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private AuthResponse login(String usernameOrEmail, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail(usernameOrEmail)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    @Test
    @DisplayName("Login and access protected resource - success")
    void loginAndAccessProtectedResource_Success() throws Exception {
        RegisterRequest registerRequest = createUniqueRegisterRequest();
        AuthResponse authResponse = register(registerRequest);

        assertThat(authResponse.getAccessToken()).isNotEmpty();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout then access with old token - forbidden")
    void logoutThenAccessWithOldToken_Forbidden() throws Exception {
        RegisterRequest registerRequest = createUniqueRegisterRequest();
        AuthResponse authResponse = register(registerRequest);
        String token = authResponse.getAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token refresh - old token invalid, new token works")
    void tokenRefresh_OldTokenInvalid_NewTokenWorks() throws Exception {
        RegisterRequest registerRequest = createUniqueRegisterRequest();
        AuthResponse authResponse = register(registerRequest);
        String oldToken = authResponse.getAccessToken();
        String refreshToken = authResponse.getRefreshToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse newAuthResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        String newToken = newAuthResponse.getAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Relogin - previous session invalidated")
    void relogin_PreviousSessionInvalidated() throws Exception {
        RegisterRequest registerRequest = createUniqueRegisterRequest();
        AuthResponse firstAuth = register(registerRequest);
        String firstToken = firstAuth.getAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk());

        AuthResponse secondAuth = login(registerRequest.getUsername(), registerRequest.getPassword());
        String secondToken = secondAuth.getAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk());
    }
}
