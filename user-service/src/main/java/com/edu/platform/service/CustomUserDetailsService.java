package com.edu.platform.service;

import com.edu.platform.entity.User;
import com.edu.platform.exception.ResourceNotFoundException;
import com.edu.platform.repository.UserRepository;
import com.edu.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户详情加载服务
 *
 * <p>两个加载方法对应两种认证场景：
 * loadUserByUsername - 登录时由 AuthenticationManager 调用，支持用户名/邮箱双方式登录；
 * loadUserById - JWT 过滤器在 Redis 缓存未命中时调用。</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 使用 @Transactional 确保 User 的 lazy-loaded 关联在会话内可用
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserPrincipal.create(user);
    }
}
