package com.edu.course.repository;

import com.edu.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程数据访问层。
 *
 * 自定义查询说明：
 * - searchByKeyword: 使用 LIKE 模糊搜索，适用于小数据量；大数据量应接入 Elasticsearch
 * - findByIdWithChapters: 使用 JOIN FETCH 避免懒加载导致的额外查询
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByStatus(Course.CourseStatus status, Pageable pageable);

    Page<Course> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    List<Course> findByInstructorIdAndStatus(Long instructorId, Course.CourseStatus status);

    @Query("SELECT c FROM Course c WHERE c.status = :status AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchByKeyword(@Param("keyword") String keyword,
                                 @Param("status") Course.CourseStatus status,
                                 Pageable pageable);

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.chapters WHERE c.id = :id")
    Optional<Course> findByIdWithChapters(@Param("id") Long id);

    boolean existsByTitleAndInstructorId(String title, Long instructorId);
}
