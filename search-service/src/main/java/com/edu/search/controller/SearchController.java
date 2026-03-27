package com.edu.search.controller;

import com.edu.search.dto.CourseSearchResponse;
import com.edu.search.dto.PageResponse;
import com.edu.search.service.CourseSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 课程搜索 REST API。
 * 所有参数均为可选，支持关键词、分类、价格区间等多维度组合搜索。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final CourseSearchService courseSearchService;

    public SearchController(CourseSearchService courseSearchService) {
        this.courseSearchService = courseSearchService;
    }

    @GetMapping("/courses")
    public ResponseEntity<PageResponse<CourseSearchResponse>> searchCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<CourseSearchResponse> result = courseSearchService.search(
            keyword, categoryId, minPrice, maxPrice, page, size);

        return ResponseEntity.ok(result);
    }
}
