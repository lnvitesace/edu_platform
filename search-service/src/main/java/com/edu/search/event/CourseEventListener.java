package com.edu.search.event;

import com.edu.search.document.CourseDocument;
import com.edu.search.service.CourseSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 课程事件消费者。
 * <p>
 * 监听 course-service 发布的课程生命周期事件，实时同步到 Elasticsearch 索引。
 * CREATED 事件被忽略——课程创建时通常为草稿状态，只有发布后才需要被搜索到。
 */
@Component
public class CourseEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CourseEventListener.class);

    private final CourseSearchService courseSearchService;

    public CourseEventListener(CourseSearchService courseSearchService) {
        this.courseSearchService = courseSearchService;
    }

    @RabbitListener(queues = "course.search.queue")
    public void handleCourseEvent(CourseEvent event) {
        logger.info("Received course event: type={}, courseId={}",
            event.getEventType(), event.getCourseId());

        try {
            switch (event.getEventType()) {
                case PUBLISHED, UPDATED -> indexCourse(event);
                case DELETED -> courseSearchService.deleteCourse(event.getCourseId());
                default -> logger.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to process course event: {}", e.getMessage(), e);
        }
    }

    private void indexCourse(CourseEvent event) {
        CourseDocument document = CourseDocument.builder()
            .id(event.getCourseId())
            .title(event.getTitle())
            .description(event.getDescription())
            .coverImage(event.getCoverImage())
            .instructorId(event.getInstructorId())
            .categoryId(event.getCategoryId())
            .categoryName(event.getCategoryName())
            .price(event.getPrice())
            .status(event.getStatus())
            .createdAt(event.getTimestamp())
            .updatedAt(LocalDateTime.now())
            .build();

        courseSearchService.indexCourse(document);
    }
}
