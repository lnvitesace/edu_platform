package com.edu.search.event;

import com.edu.search.document.CourseDocument;
import com.edu.search.dto.PageResponse;
import com.edu.search.service.CourseSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CourseEventListener Unit Tests")
class CourseEventListenerTest {

    @Test
    @DisplayName("PUBLISHED event should index course")
    void handleCourseEvent_published_indexesCourse() {
        RecordingCourseSearchService courseSearchService = new RecordingCourseSearchService();
        CourseEventListener listener = new CourseEventListener(courseSearchService);

        CourseEvent event = CourseEvent.builder()
                .courseId(1L)
                .title("Java Basics")
                .description("Learn Java")
                .coverImage("https://example.com/java.png")
                .instructorId(10L)
                .categoryId(100L)
                .categoryName("Programming")
                .price(new BigDecimal("99.00"))
                .status("PUBLISHED")
                .eventType(CourseEvent.EventType.PUBLISHED)
                .timestamp(LocalDateTime.now())
                .build();

        listener.handleCourseEvent(event);

        assertThat(courseSearchService.lastIndexedCourse).isNotNull();
        assertThat(courseSearchService.lastIndexedCourse.getId()).isEqualTo(1L);
        assertThat(courseSearchService.lastDeletedCourseId).isNull();
    }

    @Test
    @DisplayName("DELETED event should remove course from index")
    void handleCourseEvent_deleted_removesCourse() {
        RecordingCourseSearchService courseSearchService = new RecordingCourseSearchService();
        CourseEventListener listener = new CourseEventListener(courseSearchService);

        CourseEvent event = CourseEvent.builder()
                .courseId(1L)
                .eventType(CourseEvent.EventType.DELETED)
                .timestamp(LocalDateTime.now())
                .build();

        listener.handleCourseEvent(event);

        assertThat(courseSearchService.lastDeletedCourseId).isEqualTo(1L);
        assertThat(courseSearchService.lastIndexedCourse).isNull();
    }

    private static class RecordingCourseSearchService extends CourseSearchService {
        private CourseDocument lastIndexedCourse;
        private Long lastDeletedCourseId;

        RecordingCourseSearchService() {
            super(null, null);
        }

        @Override
        public PageResponse<com.edu.search.dto.CourseSearchResponse> search(String keyword, Long categoryId,
                                                                            BigDecimal minPrice, BigDecimal maxPrice,
                                                                            int page, int size) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public void indexCourse(CourseDocument course) {
            this.lastIndexedCourse = course;
        }

        @Override
        public void deleteCourse(Long courseId) {
            this.lastDeletedCourseId = courseId;
        }
    }
}
