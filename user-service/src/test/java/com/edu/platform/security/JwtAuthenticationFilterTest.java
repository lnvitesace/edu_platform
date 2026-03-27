package com.edu.platform.security;

import com.edu.platform.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String DETAILS_KEY_PREFIX = "user:details:";
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, redisTemplate, objectMapper);
        SecurityContextHolder.clearContext();

        userPrincipal = new UserPrincipal(
                USER_ID,
                "testuser",
                "test@example.com",
                "password",
                "STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    @Test
    @DisplayName("Valid token and session match - sets authentication (DB fallback)")
    void doFilter_ValidTokenAndSessionMatch_SetsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList(VALID_TOKEN, null));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(userPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userPrincipal);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Valid token but session not exist - no authentication")
    void doFilter_ValidTokenButSessionNotExist_NoAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList(null, null));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Valid token but session mismatch - no authentication")
    void doFilter_ValidTokenButSessionMismatch_NoAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList("different.jwt.token", null));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid token - no authentication, skip Redis")
    void doFilter_InvalidToken_NoAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(redisTemplate, never()).opsForValue();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Cache hit for details - does not query DB")
    void doFilter_CacheHitForDetails_DoesNotQueryDB() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList(VALID_TOKEN, "{\"id\":1}"));
        when(objectMapper.readValue(anyString(), eq(CachedUserDetails.class)))
                .thenReturn(new CachedUserDetails(USER_ID, "testuser", "test@example.com", "password", "STUDENT"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(userDetailsService, never()).loadUserById(USER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Cache miss for details - queries DB")
    void doFilter_CacheMissForDetails_QueriesDB() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList(VALID_TOKEN, null));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(userPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(userDetailsService, times(1)).loadUserById(USER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Cache corrupted - falls back to DB")
    void doFilter_CacheCorrupted_FallsBackToDb() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenReturn(USER_ID);
        when(redisTemplate.opsForValue().multiGet(
                List.of(SESSION_KEY_PREFIX + USER_ID, DETAILS_KEY_PREFIX + USER_ID)
        )).thenReturn(Arrays.asList(VALID_TOKEN, "{invalid-json}"));
        when(objectMapper.readValue(anyString(), eq(CachedUserDetails.class)))
                .thenThrow(new RuntimeException("bad json"));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(userPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(userDetailsService).loadUserById(USER_ID);
        verify(redisTemplate).delete(DETAILS_KEY_PREFIX + USER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("No Authorization header - skips authentication")
    void doFilter_NoAuthorizationHeader_SkipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).getUserIdIfValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Malformed Authorization header (Basic) - skips authentication")
    void doFilter_MalformedAuthorizationHeader_SkipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).getUserIdIfValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Empty Bearer token - skips authentication")
    void doFilter_EmptyBearerToken_SkipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).getUserIdIfValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Exception during processing - continues chain")
    void doFilter_ExceptionDuringProcessing_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.getUserIdIfValid(VALID_TOKEN)).thenThrow(new RuntimeException("Unexpected error"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
