package com.edu.course.controller;

import com.edu.course.dto.LessonRequest;
import com.edu.course.dto.LessonResponse;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.service.EnrollmentService;
import com.edu.course.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课时管理 REST API。
 *
 * URL 设计同章节，课时嵌套在章节路径下。
 * 创建课时: POST /api/courses/chapters/{chapterId}/lessons
 * 修改/删除: PUT/DELETE /api/courses/lessons/{id}
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final EnrollmentService enrollmentService;

    @GetMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<List<LessonResponse>> getLessonsByChapterId(@PathVariable Long chapterId) {
        List<LessonResponse> lessons = lessonService.getLessonsByChapterId(chapterId);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<LessonResponse> getLessonById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        LessonResponse lesson = lessonService.getLessonById(id);
        if (!Boolean.TRUE.equals(lesson.getIsFree())) {
            if (userId == null) {
                throw new ForbiddenException("You must login and enroll to access this lesson");
            }
            enrollmentService.checkLessonAccess(id, userId);
        }
        return ResponseEntity.ok(lesson);
    }

    @PostMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable Long chapterId,
            @Valid @RequestBody LessonRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        LessonResponse lesson = lessonService.createLesson(chapterId, request, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
    }

    @PutMapping("/lessons/{id}")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        LessonResponse lesson = lessonService.updateLesson(id, request, userId, userRole);
        return ResponseEntity.ok(lesson);
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        lessonService.deleteLesson(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}
