package com.edu.search.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程领域事件消息体。
 * <p>
 * 由 course-service 发布，search-service 订阅消费。
 * 实现 Serializable 以支持 RabbitMQ 的 Java 序列化（当前实际使用 Jackson JSON）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long courseId;
    private String title;
    private String description;
    private String coverImage;
    private Long instructorId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private String status;
    private EventType eventType;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED,
        PUBLISHED,
        UPDATED,
        DELETED
    }
}
