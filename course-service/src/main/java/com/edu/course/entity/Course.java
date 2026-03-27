package com.edu.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程实体。
 *
 * 核心聚合根，包含课程元数据和内容结构。
 * 采用 DRAFT -> PUBLISHED -> ARCHIVED 状态机管理课程生命周期：
 * - DRAFT: 草稿状态，仅创建者可见可编辑
 * - PUBLISHED: 已发布，对所有用户可见
 * - ARCHIVED: 已归档，从列表隐藏但保留数据
 *
 * instructorId 存储讲师用户 ID 而非关联 User 实体，因为用户数据在独立的 user-service 中。
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    // 使用 LAZY 加载分类，避免查询课程列表时产生 N+1 问题
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // orphanRemoval=true 确保删除课程时级联删除所有章节
    // OrderBy 保证章节按 sortOrder 排序返回
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<Chapter> chapters = new ArrayList<>();

    public enum CourseStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
