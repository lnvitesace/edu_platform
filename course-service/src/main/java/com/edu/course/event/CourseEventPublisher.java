package com.edu.course.event;

import com.edu.course.entity.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 课程事件发布器。
 *
 * 将课程变更事件异步发送到 RabbitMQ，解耦课程服务与搜索服务。
 * 发布失败仅记录日志不抛异常，避免消息中间件故障阻塞业务流程。
 * 这种权衡牺牲了事件投递的强一致性，换取了系统的高可用性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE_NAME = "course.exchange";

    public void publishCourseCreated(Course course) {
        publishEvent(course, CourseEvent.EventType.CREATED, "course.created");
    }

    public void publishCoursePublished(Course course) {
        publishEvent(course, CourseEvent.EventType.PUBLISHED, "course.published");
    }

    public void publishCourseUpdated(Course course) {
        publishEvent(course, CourseEvent.EventType.UPDATED, "course.updated");
    }

    public void publishCourseDeleted(Long courseId) {
        CourseEvent event = CourseEvent.builder()
                .courseId(courseId)
                .eventType(CourseEvent.EventType.DELETED)
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(event, "course.deleted");
    }

    private void publishEvent(Course course, CourseEvent.EventType eventType, String routingKey) {
        CourseEvent event = CourseEvent.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .coverImage(course.getCoverImage())
                .instructorId(course.getInstructorId())
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .price(course.getPrice())
                .status(course.getStatus().name())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(event, routingKey);
    }

    private void sendEvent(CourseEvent event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
            log.info("Published course event: type={}, courseId={}", event.getEventType(), event.getCourseId());
        } catch (Exception e) {
            log.error("Failed to publish course event: type={}, courseId={}", event.getEventType(), event.getCourseId(), e);
        }
    }
}
