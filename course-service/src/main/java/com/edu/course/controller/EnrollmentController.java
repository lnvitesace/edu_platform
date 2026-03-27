package com.edu.course.controller;

import com.edu.course.dto.EnrollmentRequest;
import com.edu.course.dto.EnrollmentResponse;
import com.edu.course.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报名管理 REST API。
 *
 * 所有端点均需认证（X-User-Id header），由网关注入。
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        EnrollmentResponse response = enrollmentService.enroll(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(
            @RequestParam Long courseId,
            @RequestHeader("X-User-Id") Long userId) {
        boolean enrolled = enrollmentService.isEnrolled(userId, courseId);
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId) {
        List<EnrollmentResponse> enrollments = enrollmentService.getMyEnrollments(userId);
        return ResponseEntity.ok(enrollments);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> cancelEnrollment(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId) {
        enrollmentService.cancelEnrollment(courseId, userId);
        return ResponseEntity.noContent().build();
    }
}
