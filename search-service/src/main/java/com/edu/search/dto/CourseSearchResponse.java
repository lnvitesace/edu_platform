package com.edu.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课程搜索结果项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSearchResponse {

    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private Long instructorId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private String status;
}
