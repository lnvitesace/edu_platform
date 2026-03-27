package com.edu.course.controller;

import com.edu.course.dto.ChapterRequest;
import com.edu.course.dto.ChapterResponse;
import com.edu.course.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 章节管理 REST API。
 *
 * URL 设计：章节资源嵌套在课程路径下，体现从属关系。
 * 创建章节: POST /api/courses/{courseId}/chapters
 * 修改/删除: PUT/DELETE /api/courses/chapters/{id} (不需要 courseId，由 id 即可定位)
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/{courseId}/chapters")
    public ResponseEntity<List<ChapterResponse>> getChaptersByCourseId(@PathVariable Long courseId) {
        List<ChapterResponse> chapters = chapterService.getChaptersByCourseId(courseId);
        return ResponseEntity.ok(chapters);
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<ChapterResponse> getChapterById(@PathVariable Long id) {
        ChapterResponse chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(chapter);
    }

    @PostMapping("/{courseId}/chapters")
    public ResponseEntity<ChapterResponse> createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody ChapterRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        ChapterResponse chapter = chapterService.createChapter(courseId, request, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(chapter);
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        ChapterResponse chapter = chapterService.updateChapter(id, request, userId, userRole);
        return ResponseEntity.ok(chapter);
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {
        chapterService.deleteChapter(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}
