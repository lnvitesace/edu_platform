package com.edu.course.service;

import com.edu.course.dto.LessonProgressResponse;
import com.edu.course.dto.ProgressRequest;
import com.edu.course.entity.Lesson;
import com.edu.course.entity.LessonProgress;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.LessonProgressRepository;
import com.edu.course.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgressService Unit Tests")
class ProgressServiceTest {

    @Mock
    private LessonProgressRepository progressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private ProgressService progressService;

    private Lesson testLesson;
    private LessonProgress existingProgress;
    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;

    @BeforeEach
    void setUp() {
        testLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Introduction to Java")
                .duration(600) // 10 minutes
                .build();

        existingProgress = LessonProgress.builder()
                .id(1L)
                .userId(USER_ID)
                .lesson(testLesson)
                .watchedSeconds(300)
                .completed(false)
                .lastWatchedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // --- updateProgress ---

    @Test
    @DisplayName("updateProgress - 首次上报创建新记录")
    void updateProgress_FirstReport_CreatesNewRecord() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(120).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.empty());
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getWatchedSeconds()).isEqualTo(120);
        assertThat(result.getCompleted()).isFalse();
        verify(progressRepository).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("updateProgress - 新值 > 旧值时更新")
    void updateProgress_HigherValue_Updates() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(450).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getWatchedSeconds()).isEqualTo(450);
    }

    @Test
    @DisplayName("updateProgress - 新值 < 旧值时不倒退")
    void updateProgress_LowerValue_DoesNotRegress() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(100).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getWatchedSeconds()).isEqualTo(300); // 保持原值
    }

    @Test
    @DisplayName("updateProgress - 达到 90% 阈值自动完成")
    void updateProgress_ReachesThreshold_AutoCompletes() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(540).build(); // 540/600 = 90%

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getCompleted()).isTrue();
        assertThat(result.getWatchedSeconds()).isEqualTo(540);
    }

    @Test
    @DisplayName("updateProgress - 未达阈值不标记完成")
    void updateProgress_BelowThreshold_NotCompleted() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(539).build(); // 539/600 < 90%

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateProgress - 已完成再上报保持完成状态")
    void updateProgress_AlreadyCompleted_StaysCompleted() {
        existingProgress.setCompleted(true);
        existingProgress.setWatchedSeconds(550);
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(560).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateProgress - duration=0 时不触发自动完成")
    void updateProgress_ZeroDuration_NoAutoComplete() {
        testLesson.setDuration(0);
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(500).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateProgress - Lesson 不存在时抛 ResourceNotFoundException")
    void updateProgress_LessonNotFound_ThrowsException() {
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(999L).watchedSeconds(100).build();

        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.updateProgress(request, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProgress - 更新 lastWatchedAt 时间戳")
    void updateProgress_UpdatesLastWatchedAt() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ProgressRequest request = ProgressRequest.builder()
                .lessonId(LESSON_ID).watchedSeconds(350).build();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));
        when(progressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonProgressResponse result = progressService.updateProgress(request, USER_ID);

        assertThat(result.getLastWatchedAt()).isAfter(before);
    }

    // --- getLessonProgress ---

    @Test
    @DisplayName("getLessonProgress - 返回已有进度")
    void getLessonProgress_Exists_ReturnsProgress() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.of(existingProgress));

        LessonProgressResponse result = progressService.getLessonProgress(LESSON_ID, USER_ID);

        assertThat(result.getLessonId()).isEqualTo(LESSON_ID);
        assertThat(result.getWatchedSeconds()).isEqualTo(300);
        assertThat(result.getTotalDuration()).isEqualTo(600);
    }

    @Test
    @DisplayName("getLessonProgress - 无记录返回默认值")
    void getLessonProgress_NoRecord_ReturnsDefault() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(testLesson));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.empty());

        LessonProgressResponse result = progressService.getLessonProgress(LESSON_ID, USER_ID);

        assertThat(result.getLessonId()).isEqualTo(LESSON_ID);
        assertThat(result.getWatchedSeconds()).isEqualTo(0);
        assertThat(result.getCompleted()).isFalse();
        assertThat(result.getTotalDuration()).isEqualTo(600);
    }

    @Test
    @DisplayName("getLessonProgress - Lesson 不存在时抛异常")
    void getLessonProgress_LessonNotFound_ThrowsException() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.getLessonProgress(999L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
