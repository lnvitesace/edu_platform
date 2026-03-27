package com.edu.course.controller;

import com.edu.course.dto.LessonProgressResponse;
import com.edu.course.dto.ProgressRequest;
import com.edu.course.service.EnrollmentService;
import com.edu.course.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 学习进度 REST API。
 *
 * 所有端点均需认证（X-User-Id header），由网关注入。
 * 写操作需验证用户已报名对应课程。
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<LessonProgressResponse> updateProgress(
            @Valid @RequestBody ProgressRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        enrollmentService.checkLessonAccess(request.getLessonId(), userId);
        LessonProgressResponse response = progressService.updateProgress(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<LessonProgressResponse> getLessonProgress(
            @PathVariable Long lessonId,
            @RequestHeader("X-User-Id") Long userId) {
        LessonProgressResponse response = progressService.getLessonProgress(lessonId, userId);
        return ResponseEntity.ok(response);
    }
}
