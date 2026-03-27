package com.edu.course.service;

import com.edu.course.dto.LessonRequest;
import com.edu.course.dto.LessonResponse;
import com.edu.course.entity.Chapter;
import com.edu.course.entity.Course;
import com.edu.course.entity.Lesson;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonService Unit Tests")
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private LessonService lessonService;

    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private LessonRequest lessonRequest;

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
                .build();

        testLesson = Lesson.builder()
                .id(1L)
                .chapter(testChapter)
                .title("Hello World")
                .videoUrl("http://example.com/video.mp4")
                .duration(600)
                .isFree(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lessonRequest = LessonRequest.builder()
                .title("Hello World")
                .videoUrl("http://example.com/video.mp4")
                .duration(600)
                .isFree(true)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("getLessonsByChapterId - Returns lessons ordered by sort")
    void getLessonsByChapterId_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.findByChapterIdOrderBySortOrderAsc(1L)).thenReturn(List.of(testLesson));

        List<LessonResponse> result = lessonService.getLessonsByChapterId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("getLessonsByChapterId - Throws exception when chapter not found")
    void getLessonsByChapterId_ChapterNotFound() {
        when(chapterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLessonsByChapterId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getLessonById - Returns lesson successfully")
    void getLessonById_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));

        LessonResponse result = lessonService.getLessonById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Hello World");
        assertThat(result.getIsFree()).isTrue();
    }

    @Test
    @DisplayName("getLessonById - Throws exception when not found")
    void getLessonById_NotFound() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLessonById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createLesson - Creates lesson successfully")
    void createLesson_Success() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);

        LessonResponse result = lessonService.createLesson(1L, lessonRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("createLesson - Auto-assigns sort order when null")
    void createLesson_AutoSortOrder() {
        lessonRequest.setSortOrder(null);
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(false);
        when(lessonRepository.findMaxSortOrderByChapterId(1L)).thenReturn(3);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            assertThat(l.getSortOrder()).isEqualTo(4);
            return testLesson;
        });

        lessonService.createLesson(1L, lessonRequest, 100L, "INSTRUCTOR");

        verify(lessonRepository).findMaxSortOrderByChapterId(1L);
    }

    @Test
    @DisplayName("createLesson - Auto-assigns sort order when no existing lessons")
    void createLesson_AutoSortOrderWhenNoExisting() {
        lessonRequest.setSortOrder(null);
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(false);
        when(lessonRepository.findMaxSortOrderByChapterId(1L)).thenReturn(null);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            assertThat(l.getSortOrder()).isEqualTo(1);
            return testLesson;
        });

        lessonService.createLesson(1L, lessonRequest, 100L, "INSTRUCTOR");

        verify(lessonRepository).findMaxSortOrderByChapterId(1L);
    }

    @Test
    @DisplayName("createLesson - Uses default values for null fields")
    void createLesson_DefaultValues() {
        lessonRequest.setDuration(null);
        lessonRequest.setIsFree(null);
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            assertThat(l.getDuration()).isEqualTo(0);
            assertThat(l.getIsFree()).isFalse();
            return testLesson;
        });

        lessonService.createLesson(1L, lessonRequest, 100L, "INSTRUCTOR");

        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("createLesson - Throws exception when title exists")
    void createLesson_TitleExists() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(true);

        assertThatThrownBy(() -> lessonService.createLesson(1L, lessonRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createLesson - Throws forbidden when not owner")
    void createLesson_ForbiddenNotOwner() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));

        assertThatThrownBy(() -> lessonService.createLesson(1L, lessonRequest, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("createLesson - Admin can create lesson")
    void createLesson_AdminSuccess() {
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(testChapter));
        when(lessonRepository.existsByTitleAndChapterId("Hello World", 1L)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);

        LessonResponse result = lessonService.createLesson(1L, lessonRequest, 999L, "ADMIN");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateLesson - Updates lesson successfully")
    void updateLesson_Success() {
        LessonRequest updateRequest = LessonRequest.builder()
                .title("Updated Title")
                .videoUrl("http://example.com/new.mp4")
                .duration(1200)
                .isFree(false)
                .sortOrder(2)
                .build();

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.existsByTitleAndChapterIdAndIdNot("Updated Title", 1L, 1L)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);

        LessonResponse result = lessonService.updateLesson(1L, updateRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("updateLesson - Throws exception when not found")
    void updateLesson_NotFound() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.updateLesson(999L, lessonRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateLesson - Throws forbidden when not owner")
    void updateLesson_ForbiddenNotOwner() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));

        assertThatThrownBy(() -> lessonService.updateLesson(1L, lessonRequest, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("updateLesson - Throws exception when title exists")
    void updateLesson_TitleExists() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.existsByTitleAndChapterIdAndIdNot("Hello World", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> lessonService.updateLesson(1L, lessonRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateLesson - Does not update null fields")
    void updateLesson_NullFields() {
        LessonRequest partialRequest = LessonRequest.builder()
                .title("Updated Title")
                .build();

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.existsByTitleAndChapterIdAndIdNot("Updated Title", 1L, 1L)).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);

        lessonService.updateLesson(1L, partialRequest, 100L, "INSTRUCTOR");

        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("deleteLesson - Deletes lesson successfully")
    void deleteLesson_Success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));

        lessonService.deleteLesson(1L, 100L, "INSTRUCTOR");

        verify(lessonRepository).delete(testLesson);
    }

    @Test
    @DisplayName("deleteLesson - Throws exception when not found")
    void deleteLesson_NotFound() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.deleteLesson(999L, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteLesson - Throws forbidden when not owner")
    void deleteLesson_ForbiddenNotOwner() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(testLesson));

        assertThatThrownBy(() -> lessonService.deleteLesson(1L, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }
}
