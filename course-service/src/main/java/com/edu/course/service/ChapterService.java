package com.edu.course.service;

import com.edu.course.dto.ChapterRequest;
import com.edu.course.dto.ChapterResponse;
import com.edu.course.dto.LessonResponse;
import com.edu.course.entity.Chapter;
import com.edu.course.entity.Course;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 章节业务逻辑层。
 *
 * 章节操作需要校验对父课程的所有权，确保只有课程创建者能管理章节。
 * sortOrder 自动递增逻辑：新建章节时若未指定排序值，自动取最大值+1。
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByCourseId(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        return chapters.stream()
                .map(this::mapToChapterResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findByIdWithLessons(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));
        return mapToChapterResponseWithLessons(chapter);
    }

    @Transactional
    public ChapterResponse createChapter(Long courseId, ChapterRequest request, Long userId, String userRole) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        checkCourseOwnership(course, userId, userRole);

        if (chapterRepository.existsByTitleAndCourseId(request.getTitle(), courseId)) {
            throw new BadRequestException("Chapter with title '" + request.getTitle() + "' already exists in this course");
        }

        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            Integer maxSortOrder = chapterRepository.findMaxSortOrderByCourseId(courseId);
            sortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 1;
        }

        Chapter chapter = Chapter.builder()
                .course(course)
                .title(request.getTitle())
                .sortOrder(sortOrder)
                .build();

        chapter = chapterRepository.save(chapter);
        return mapToChapterResponse(chapter);
    }

    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request, Long userId, String userRole) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        checkCourseOwnership(chapter.getCourse(), userId, userRole);

        if (chapterRepository.existsByTitleAndCourseIdAndIdNot(request.getTitle(), chapter.getCourse().getId(), id)) {
            throw new BadRequestException("Chapter with title '" + request.getTitle() + "' already exists in this course");
        }

        chapter.setTitle(request.getTitle());
        if (request.getSortOrder() != null) {
            chapter.setSortOrder(request.getSortOrder());
        }

        chapter = chapterRepository.save(chapter);
        return mapToChapterResponse(chapter);
    }

    @Transactional
    public void deleteChapter(Long id, Long userId, String userRole) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        checkCourseOwnership(chapter.getCourse(), userId, userRole);

        chapterRepository.delete(chapter);
    }

    private void checkCourseOwnership(Course course, Long userId, String userRole) {
        if (!"ADMIN".equals(userRole) && !course.getInstructorId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to modify this course");
        }
    }

    private ChapterResponse mapToChapterResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourse().getId())
                .title(chapter.getTitle())
                .sortOrder(chapter.getSortOrder())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }

    private ChapterResponse mapToChapterResponseWithLessons(Chapter chapter) {
        List<LessonResponse> lessons = chapter.getLessons() != null
                ? chapter.getLessons().stream()
                    .map(lesson -> LessonResponse.builder()
                            .id(lesson.getId())
                            .chapterId(chapter.getId())
                            .title(lesson.getTitle())
                            .videoUrl(lesson.getVideoUrl())
                            .duration(lesson.getDuration())
                            .isFree(lesson.getIsFree())
                            .sortOrder(lesson.getSortOrder())
                            .createdAt(lesson.getCreatedAt())
                            .updatedAt(lesson.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return ChapterResponse.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourse().getId())
                .title(chapter.getTitle())
                .sortOrder(chapter.getSortOrder())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .lessons(lessons)
                .build();
    }
}
