package com.edu.course.service;

import com.edu.course.dto.LessonRequest;
import com.edu.course.dto.LessonResponse;
import com.edu.course.entity.Chapter;
import com.edu.course.entity.Lesson;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课时业务逻辑层。
 *
 * 课时是课程内容的最小单元，权限校验需要向上追溯到课程层级。
 * 注意：修改课时不会触发课程缓存失效，因为课程详情缓存包含完整的章节课时结构，
 * 生产环境应考虑在课时变更时也清理对应课程的缓存。
 */
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByChapterId(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        List<Lesson> lessons = lessonRepository.findByChapterIdOrderBySortOrderAsc(chapterId);
        return lessons.stream()
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        return mapToLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(Long chapterId, LessonRequest request, Long userId, String userRole) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        checkCourseOwnership(chapter, userId, userRole);

        if (lessonRepository.existsByTitleAndChapterId(request.getTitle(), chapterId)) {
            throw new BadRequestException("Lesson with title '" + request.getTitle() + "' already exists in this chapter");
        }

        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            Integer maxSortOrder = lessonRepository.findMaxSortOrderByChapterId(chapterId);
            sortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 1;
        }

        Lesson lesson = Lesson.builder()
                .chapter(chapter)
                .title(request.getTitle())
                .videoUrl(request.getVideoUrl())
                .duration(request.getDuration() != null ? request.getDuration() : 0)
                .isFree(request.getIsFree() != null ? request.getIsFree() : false)
                .sortOrder(sortOrder)
                .build();

        lesson = lessonRepository.save(lesson);
        return mapToLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest request, Long userId, String userRole) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));

        checkCourseOwnership(lesson.getChapter(), userId, userRole);

        if (lessonRepository.existsByTitleAndChapterIdAndIdNot(request.getTitle(), lesson.getChapter().getId(), id)) {
            throw new BadRequestException("Lesson with title '" + request.getTitle() + "' already exists in this chapter");
        }

        lesson.setTitle(request.getTitle());
        lesson.setVideoUrl(request.getVideoUrl());
        if (request.getDuration() != null) {
            lesson.setDuration(request.getDuration());
        }
        if (request.getIsFree() != null) {
            lesson.setIsFree(request.getIsFree());
        }
        if (request.getSortOrder() != null) {
            lesson.setSortOrder(request.getSortOrder());
        }

        lesson = lessonRepository.save(lesson);
        return mapToLessonResponse(lesson);
    }

    @Transactional
    public void deleteLesson(Long id, Long userId, String userRole) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));

        checkCourseOwnership(lesson.getChapter(), userId, userRole);

        lessonRepository.delete(lesson);
    }

    /**
     * 权限校验通过 chapter -> course 链路追溯到课程所有者。
     * 这种间接关联避免了在 Lesson 实体上冗余存储 instructorId。
     */
    private void checkCourseOwnership(Chapter chapter, Long userId, String userRole) {
        if (!"ADMIN".equals(userRole) && !chapter.getCourse().getInstructorId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to modify this course");
        }
    }

    private LessonResponse mapToLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .chapterId(lesson.getChapter().getId())
                .title(lesson.getTitle())
                .videoUrl(lesson.getVideoUrl())
                .duration(lesson.getDuration())
                .isFree(lesson.getIsFree())
                .sortOrder(lesson.getSortOrder())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}
