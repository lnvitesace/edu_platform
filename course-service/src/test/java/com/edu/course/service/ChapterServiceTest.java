package com.edu.course.service;

import com.edu.course.dto.ChapterRequest;
import com.edu.course.dto.ChapterResponse;
import com.edu.course.entity.Chapter;
import com.edu.course.entity.Course;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChapterService Unit Tests")
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ChapterService chapterService;

    private Course testCourse;
    private Chapter testChapter;
    private ChapterRequest chapterRequest;

    @BeforeEach
    void setUp() {
        testCourse = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructorId(100L)
                .status(Course.CourseStatus.DRAFT)
                .build();

        testChapter = Chapter.builder()
                .id(1L)
                .course(testCourse)
                .title("Introduction")
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lessons(new ArrayList<>())
                .build();

        chapterRequest = ChapterRequest.builder()
                .title("Introduction")
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("getChaptersByCourseId - Returns chapters ordered by sort")
    void getChaptersByCourseId_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.findByCourseIdOrderBySortOrderAsc(1L)).thenReturn(List.of(testChapter));

        List<ChapterResponse> result = chapterService.getChaptersByCourseId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Introduction");
    }

    @Test
    @DisplayName("getChaptersByCourseId - Throws exception when course not found")
    void getChaptersByCourseId_CourseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.getChaptersByCourseId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getChapterById - Returns chapter with lessons")
    void getChapterById_Success() {
        when(chapterRepository.findByIdWithLessons(1L)).thenReturn(Optional.of(testChapter));

        ChapterResponse result = chapterService.getChapterById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Introduction");
    }

    @Test
    @DisplayName("getChapterById - Throws exception when not found")
    void getChapterById_NotFound() {
        when(chapterRepository.findByIdWithLessons(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.getChapterById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createChapter - Creates chapter successfully")
    void createChapter_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.existsByTitleAndCourseId("Introduction", 1L)).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);

        ChapterResponse result = chapterService.createChapter(1L, chapterRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Introduction");
    }

    @Test
    @DisplayName("createChapter - Auto-assigns sort order when null")
    void createChapter_AutoSortOrder() {
        chapterRequest.setSortOrder(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.existsByTitleAndCourseId("Introduction", 1L)).thenReturn(false);
        when(chapterRepository.findMaxSortOrderByCourseId(1L)).thenReturn(5);
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> {
            Chapter c = inv.getArgument(0);
            assertThat(c.getSortOrder()).isEqualTo(6);
            return testChapter;
        });

        chapterService.createChapter(1L, chapterRequest, 100L, "INSTRUCTOR");

        verify(chapterRepository).findMaxSortOrderByCourseId(1L);
    }

    @Test
    @DisplayName("createChapter - Auto-assigns sort order when no existing chapters")
    void createChapter_AutoSortOrderWhenNoExisting() {
        chapterRequest.setSortOrder(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.existsByTitleAndCourseId("Introduction", 1L)).thenReturn(false);
        when(chapterRepository.findMaxSortOrderByCourseId(1L)).thenReturn(null);
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> {
            Chapter c = inv.getArgument(0);
            assertThat(c.getSortOrder()).isEqualTo(1);
            return testChapter;
        });

        chapterService.createChapter(1L, chapterRequest, 100L, "INSTRUCTOR");

        verify(chapterRepository).findMaxSortOrderByCourseId(1L);
    }

    @Test
    @DisplayName("createChapter - Throws exception when title exists")
    void createChapter_TitleExists() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.existsByTitleAndCourseId("Introduction", 1L)).thenReturn(true);

        assertThatThrownBy(() -> chapterService.createChapter(1L, chapterRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createChapter - Throws forbidden when not owner")
    void createChapter_ForbiddenNotOwner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> chapterService.createChapter(1L, chapterRequest, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("createChapter - Admin can create chapter")
    void createChapter_AdminSuccess() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.existsByTitleAndCourseId("Introduction", 1L)).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);

        ChapterResponse result = chapterService.createChapter(1L, chapterRequest, 999L, "ADMIN");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateChapter - Updates chapter successfully")
    void updateChapter_Success() {
        ChapterRequest updateRequest = ChapterRequest.builder()
                .title("Updated Title")
                .sortOrder(2)
                .build();

        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(chapterRepository.existsByTitleAndCourseIdAndIdNot("Updated Title", 1L, 1L)).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);

        ChapterResponse result = chapterService.updateChapter(1L, updateRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    @DisplayName("updateChapter - Throws exception when title exists")
    void updateChapter_TitleExists() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(chapterRepository.existsByTitleAndCourseIdAndIdNot("Introduction", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> chapterService.updateChapter(1L, chapterRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateChapter - Does not update sortOrder when null")
    void updateChapter_NullSortOrder() {
        chapterRequest.setSortOrder(null);
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(chapterRepository.existsByTitleAndCourseIdAndIdNot("Introduction", 1L, 1L)).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);

        chapterService.updateChapter(1L, chapterRequest, 100L, "INSTRUCTOR");

        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    @DisplayName("deleteChapter - Deletes chapter successfully")
    void deleteChapter_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));

        chapterService.deleteChapter(1L, 100L, "INSTRUCTOR");

        verify(chapterRepository).delete(testChapter);
    }

    @Test
    @DisplayName("deleteChapter - Throws exception when not found")
    void deleteChapter_NotFound() {
        when(chapterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.deleteChapter(999L, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteChapter - Throws forbidden when not owner")
    void deleteChapter_ForbiddenNotOwner() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));

        assertThatThrownBy(() -> chapterService.deleteChapter(1L, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }
}
