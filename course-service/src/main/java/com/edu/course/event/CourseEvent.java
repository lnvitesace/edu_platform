package com.edu.course.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程领域事件消息体。
 *
 * 实现 Serializable 以支持 RabbitMQ 的消息序列化。
 * 冗余存储课程核心字段（title/description 等），避免消费者需要反查数据库。
 * DELETE 事件只需 courseId，其余字段为 null。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEvent implements Serializable {

    @Serial
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
