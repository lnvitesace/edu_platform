package com.edu.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程响应 DTO。
 *
 * 包含嵌套的讲师信息（InstructorInfo），通过 Feign 调用 user-service 获取。
 * chapters 字段仅在获取课程详情时填充，列表查询时为 null 以减少数据量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private BigDecimal price;
    private Long instructorId;
    private InstructorInfo instructor;
    private Long categoryId;
    private String categoryName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterResponse> chapters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstructorInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
    }
}
