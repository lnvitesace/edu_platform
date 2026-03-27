package com.edu.search.controller;

import com.edu.search.dto.CourseSearchResponse;
import com.edu.search.dto.PageResponse;
import com.edu.search.service.CourseSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CourseSearchService service = new StubCourseSearchService();
        SearchController searchController = new SearchController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    @DisplayName("GET /api/search/courses should return paginated search results")
    void searchCourses_returnsResults() throws Exception {
        mockMvc.perform(get("/api/search/courses")
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Programming"))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }

    private static class StubCourseSearchService extends CourseSearchService {
        StubCourseSearchService() {
            super(null, null);
        }

        @Override
        public PageResponse<CourseSearchResponse> search(String keyword, Long categoryId,
                                                         BigDecimal minPrice, BigDecimal maxPrice,
                                                         int page, int size) {
            return PageResponse.<CourseSearchResponse>builder()
                    .content(List.of(CourseSearchResponse.builder()
                            .id(1L)
                            .title("Java Basics")
                            .categoryName("Programming")
                            .price(new BigDecimal("99.00"))
                            .status("PUBLISHED")
                            .build()))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();
        }
    }
}
