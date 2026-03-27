package com.edu.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课程搜索请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
}
