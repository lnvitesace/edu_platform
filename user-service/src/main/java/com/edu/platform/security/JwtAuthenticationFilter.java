package com.edu.platform.security;

import com.edu.platform.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * <p>继承 OncePerRequestFilter 保证每个请求只执行一次（避免 forward/include 重复过滤）。</p>
 *
 * <p>认证策略采用"双重验证"：JWT 签名验证 + Redis 会话验证。
 * 仅靠 JWT 签名无法实现即时登出（JWT 是无状态的），
 * 所以需要 Redis 存储活跃 session，登出时删除即可使 token 立即失效。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String DETAILS_KEY_PREFIX = "user:details:";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Long userId = tokenProvider.getUserIdIfValid(jwt);
                if (userId == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Redis 会话验证：确保 token 未被撤销（登出后 session key 被删除）
                String sessionKey = SESSION_KEY_PREFIX + userId;
                String detailsKey = DETAILS_KEY_PREFIX + userId;
                List<String> values = redisTemplate.opsForValue().multiGet(List.of(sessionKey, detailsKey));
                String cachedToken = values != null ? values.getFirst() : null;
                if (cachedToken == null || !cachedToken.equals(jwt)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 优先从 Redis 加载 UserDetails，减少 DB 查询；缓存未命中则回退到 DB
                UserDetails userDetails;
                String detailsJson = values.get(1);
                if (detailsJson != null) {
                    try {
                        CachedUserDetails cached = objectMapper.readValue(detailsJson, CachedUserDetails.class);
                        userDetails = cached.toUserPrincipal();
                    } catch (Exception e) {
                        redisTemplate.delete(detailsKey);
                        userDetails = userDetailsService.loadUserById(userId);
                    }
                } else {
                    userDetails = userDetailsService.loadUserById(userId);
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 认证失败不阻断请求，由后续 SecurityFilterChain 的 authorizeHttpRequests 处理 403/401
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
