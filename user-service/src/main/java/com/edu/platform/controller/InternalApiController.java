package com.edu.platform.controller;

import com.edu.platform.dto.UserResponse;
import com.edu.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 内部 API - 微服务间调用
 *
 * <p>该接口通过共享的服务间令牌认证，不经由公网暴露。</p>
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final UserService userService;

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
