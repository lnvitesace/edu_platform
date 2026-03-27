package com.edu.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课程创建/更新请求 DTO。
 * instructorId 不在此处传入，由网关从 JWT 中提取并注入 X-User-Id 请求头。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 200, message = "Course title must not exceed 200 characters")
    private String title;

    private String description;

    private String coverImage;

    @DecimalMin(value = "0.0", inclusive = true, message = "Course price must be greater than or equal to 0")
    @Digits(integer = 8, fraction = 2, message = "Course price must have at most 8 integer digits and 2 decimal places")
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    private Long categoryId;
}
