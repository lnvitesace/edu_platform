package com.edu.course.service;

import com.edu.course.dto.LessonProgressResponse;
import com.edu.course.dto.ProgressRequest;
import com.edu.course.entity.Lesson;
import com.edu.course.entity.LessonProgress;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.LessonProgressRepository;
import com.edu.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 学习进度业务逻辑层。
 *
 * 处理进度上报、进度查询，自动判定课时完成状态（观看 >= 90% 时长）。
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final double COMPLETION_THRESHOLD = 0.9;

    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public LessonProgressResponse updateProgress(ProgressRequest request, Long userId) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));

        // Cap watchedSeconds to lesson duration to prevent fake completion
        int watchedSeconds = lesson.getDuration() > 0
                ? Math.min(request.getWatchedSeconds(), lesson.getDuration())
                : request.getWatchedSeconds();

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, request.getLessonId())
                .orElseGet(() -> LessonProgress.builder()
                        .userId(userId)
                        .lesson(lesson)
                        .build());

        if (watchedSeconds > progress.getWatchedSeconds()) {
            progress.setWatchedSeconds(watchedSeconds);
        }
        // 自动完成：观看 >= 90% 时长且 duration > 0
        if (!progress.getCompleted() && lesson.getDuration() > 0
                && progress.getWatchedSeconds() >= lesson.getDuration() * COMPLETION_THRESHOLD) {
            progress.setCompleted(true);
        }
        // 更新最后观看时间
        progress.setLastWatchedAt(LocalDateTime.now());

        progressRepository.save(progress);
        return mapToResponse(progress);
    }

    @Transactional(readOnly = true)
    public LessonProgressResponse getLessonProgress(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        return progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .map(this::mapToResponse)
                .orElseGet(() -> LessonProgressResponse.builder()
                        .lessonId(lessonId)
                        .watchedSeconds(0)
                        .completed(false)
                        .totalDuration(lesson.getDuration())
                        .build());
    }

    private LessonProgressResponse mapToResponse(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .lessonId(progress.getLesson().getId())
                .watchedSeconds(progress.getWatchedSeconds())
                .completed(progress.getCompleted())
                .lastWatchedAt(progress.getLastWatchedAt())
                .totalDuration(progress.getLesson().getDuration())
                .build();
    }
}
