package com.edu.course.repository;

import com.edu.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课时数据访问层。
 *
 * findFreeLessonsByCourseId 用于获取课程的免费试看课时列表，
 * 支持未购买用户预览课程内容。
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByChapterIdOrderBySortOrderAsc(Long chapterId);

    @Query("SELECT COALESCE(MAX(l.sortOrder), 0) FROM Lesson l WHERE l.chapter.id = :chapterId")
    Integer findMaxSortOrderByChapterId(@Param("chapterId") Long chapterId);

    @Query("SELECT l FROM Lesson l WHERE l.chapter.course.id = :courseId AND l.isFree = true ORDER BY l.sortOrder ASC")
    List<Lesson> findFreeLessonsByCourseId(@Param("courseId") Long courseId);

    boolean existsByTitleAndChapterId(String title, Long chapterId);

    boolean existsByTitleAndChapterIdAndIdNot(String title, Long chapterId, Long id);
}
