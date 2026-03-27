package com.edu.search.service;

import com.edu.search.document.CourseDocument;
import com.edu.search.dto.CourseSearchResponse;
import com.edu.search.dto.PageResponse;
import com.edu.search.exception.SearchException;
import com.edu.search.repository.CourseSearchRepository;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程搜索服务。
 * <p>
 * 使用 Elasticsearch 的 bool 查询构建复合条件：
 * - must: 关键词多字段匹配（title 权重 2 倍于 description）
 * - filter: 分类、价格区间、发布状态（filter 子句不计算相关性分数，性能更优）
 */
@Service
public class CourseSearchService {

    private static final Logger logger = LoggerFactory.getLogger(CourseSearchService.class);

    private final CourseSearchRepository courseSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public CourseSearchService(CourseSearchRepository courseSearchRepository,
                               ElasticsearchOperations elasticsearchOperations) {
        this.courseSearchRepository = courseSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public PageResponse<CourseSearchResponse> search(String keyword, Long categoryId,
                                                      BigDecimal minPrice, BigDecimal maxPrice,
                                                      int page, int size) {
        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            if (keyword != null && !keyword.isBlank()) {
                boolQueryBuilder.must(Query.of(q -> q
                    .multiMatch(m -> m
                        .query(keyword)
                        .fields("title^2", "description")
                    )
                ));
            }

            if (categoryId != null) {
                boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t
                        .field("categoryId")
                        .value(categoryId)
                    )
                ));
            }

            if (minPrice != null || maxPrice != null) {
                boolQueryBuilder.filter(Query.of(q -> q
                    .range(r -> r
                        .number(n -> {
                            var numRange = n.field("price");
                            if (minPrice != null) {
                                numRange.gte(minPrice.doubleValue());
                            }
                            if (maxPrice != null) {
                                numRange.lte(maxPrice.doubleValue());
                            }
                            return numRange;
                        })
                    )
                ));
            }

            // 只搜索已发布的课程，未发布的课程对用户不可见
            boolQueryBuilder.filter(Query.of(q -> q
                .term(t -> t
                    .field("status")
                    .value("PUBLISHED")
                )
            ));

            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(PageRequest.of(page, size))
                .build();

            SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(
                searchQuery, CourseDocument.class);

            List<CourseSearchResponse> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();

            long totalElements = searchHits.getTotalHits();
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return PageResponse.<CourseSearchResponse>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= Math.max(totalPages - 1, 0))
                .build();

        } catch (Exception e) {
            logger.error("Search failed: {}", e.getMessage(), e);
            throw new SearchException("Search operation failed", e);
        }
    }

    public void indexCourse(CourseDocument course) {
        try {
            courseSearchRepository.save(course);
            logger.info("Indexed course: {}", course.getId());
        } catch (Exception e) {
            logger.error("Failed to index course {}: {}", course.getId(), e.getMessage(), e);
            throw new SearchException("Failed to index course", e);
        }
    }

    public void deleteCourse(Long courseId) {
        try {
            courseSearchRepository.deleteById(courseId);
            logger.info("Deleted course from index: {}", courseId);
        } catch (Exception e) {
            logger.error("Failed to delete course {} from index: {}", courseId, e.getMessage(), e);
            throw new SearchException("Failed to delete course from index", e);
        }
    }

    private CourseSearchResponse toResponse(CourseDocument document) {
        return CourseSearchResponse.builder()
            .id(document.getId())
            .title(document.getTitle())
            .description(document.getDescription())
            .coverImage(document.getCoverImage())
            .instructorId(document.getInstructorId())
            .categoryId(document.getCategoryId())
            .categoryName(document.getCategoryName())
            .price(document.getPrice())
            .status(document.getStatus())
            .build();
    }
}
