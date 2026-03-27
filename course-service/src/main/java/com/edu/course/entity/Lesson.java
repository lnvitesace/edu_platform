package com.edu.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 课时实体。
 *
 * 课程内容的最小单元，通常对应一个视频。
 * isFree 标记免费试看课时，用于未购买用户的课程预览。
 * duration 存储视频时长（秒），用于前端显示和学习进度统计。
 */
@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "video_url")
    private String videoUrl;

    @Column
    @Builder.Default
    private Integer duration = 0;

    @Column(name = "is_free")
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
