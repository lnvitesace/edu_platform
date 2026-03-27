package com.edu.search.repository;

import com.edu.search.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 课程搜索仓库。
 * 仅用于基础 CRUD，复杂查询通过 ElasticsearchOperations 构建 NativeQuery。
 */
@Repository
public interface CourseSearchRepository extends ElasticsearchRepository<CourseDocument, Long> {
}
