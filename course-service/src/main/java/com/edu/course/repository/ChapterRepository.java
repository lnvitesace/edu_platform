package com.edu.course.repository;

import com.edu.course.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 章节数据访问层。
 *
 * findMaxSortOrderByCourseId 用于新建章节时自动分配排序值。
 * findByCourseIdWithLessons 一次性加载章节及其课时，避免 N+1 查询。
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByCourseIdOrderBySortOrderAsc(Long courseId);

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Chapter c WHERE c.course.id = :courseId")
    Integer findMaxSortOrderByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.lessons WHERE c.id = :id")
    Optional<Chapter> findByIdWithLessons(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Chapter c LEFT JOIN FETCH c.lessons WHERE c.course.id = :courseId ORDER BY c.sortOrder ASC")
    List<Chapter> findByCourseIdWithLessons(@Param("courseId") Long courseId);

    boolean existsByTitleAndCourseId(String title, Long courseId);

    boolean existsByTitleAndCourseIdAndIdNot(String title, Long courseId, Long id);
}
