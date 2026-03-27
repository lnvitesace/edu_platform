package com.edu.platform.controller;

import com.edu.platform.dto.AuthResponse;
import com.edu.platform.dto.LoginRequest;
import com.edu.platform.dto.RegisterRequest;
import com.edu.platform.security.UserPrincipal;
import com.edu.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 用户注册、登录、登出、Token 刷新
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、登出和 Token 刷新")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册", description = "创建新用户账号，返回 JWT Token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效或用户名/邮箱已存在")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody @Parameter(description = "注册信息") RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "用户登录", description = "使用用户名/邮箱和密码登录，返回 JWT Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody @Parameter(description = "登录凭证") LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 获取新的 Access Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh Token 无效或已过期")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody @Parameter(description = "包含 refreshToken 字段") Map<String, String> request) {
        return ResponseEntity.ok(userService.refreshToken(request.get("refreshToken")));
    }

    @Operation(summary = "用户登出", description = "注销当前会话，清除 Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登出成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal @Parameter(hidden = true) UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        userService.logout(userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
