package com.edu.course.controller;

import com.edu.course.dto.CourseRequest;
import com.edu.course.dto.CourseResponse;
import com.edu.course.dto.PageResponse;
import com.edu.course.service.CourseService;
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
import org.springframework.web.bind.annotation.*;

/**
 * 课程管理 REST API。
 *
 * 认证机制：课程服务本身不做 JWT 验证，由 API Gateway 完成鉴权后
 * 将用户信息通过 X-User-Id 和 X-User-Role 请求头传递到本服务。
 * 读接口（GET）无需认证，写接口（POST/PUT/DELETE）需要 X-User-Id 头。
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理", description = "课程的增删改查、发布和归档操作")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "获取课程列表", description = "分页获取所有已发布的课程")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public ResponseEntity<PageResponse<CourseResponse>> getAllCourses(
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向: asc/desc") @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(courseService.getAllCourses(page, size, sortBy, sortDir));
    }

    @Operation(summary = "按分类查询课程", description = "获取指定分类下的课程列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<CourseResponse>> getCoursesByCategory(
            @Parameter(description = "分类 ID") @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(categoryId, page, size));
    }

    @Operation(summary = "按讲师查询课程", description = "获取指定讲师的课程列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<PageResponse<CourseResponse>> getCoursesByInstructor(
            @Parameter(description = "讲师 ID") @PathVariable Long instructorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(instructorId, page, size));
    }

    @Operation(summary = "搜索课程", description = "根据关键词搜索课程标题和描述")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<CourseResponse>> searchCourses(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseService.searchCourses(keyword, page, size));
    }

    @Operation(summary = "获取课程详情", description = "获取课程完整信息，包含章节和课时")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(
            @Parameter(description = "课程 ID") @PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @Operation(summary = "创建课程", description = "创建新课程（需要讲师权限）")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest request,
            @Parameter(description = "用户 ID（由网关注入）", hidden = true)
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request, userId));
    }

    @Operation(summary = "更新课程", description = "更新课程信息（仅课程创建者或管理员）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "403", description = "无权限修改此课程"),
            @ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @Parameter(description = "课程 ID") @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        return ResponseEntity.ok(courseService.updateCourse(id, request, userId, userRole));
    }

    @Operation(summary = "发布课程", description = "将草稿课程设为已发布状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @PutMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publishCourse(
            @Parameter(description = "课程 ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        return ResponseEntity.ok(courseService.publishCourse(id, userId, userRole));
    }

    @Operation(summary = "归档课程", description = "将课程设为归档状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "归档成功"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @PutMapping("/{id}/archive")
    public ResponseEntity<CourseResponse> archiveCourse(
            @Parameter(description = "课程 ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        return ResponseEntity.ok(courseService.archiveCourse(id, userId, userRole));
    }

    @Operation(summary = "删除课程", description = "删除课程及其所有章节和课时")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @Parameter(description = "课程 ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        courseService.deleteCourse(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}
