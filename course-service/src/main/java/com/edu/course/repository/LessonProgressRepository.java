package com.edu.course.repository;

import com.edu.course.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课时进度数据访问层。
 *
 * 提供单课时进度查询和批量进度统计，支持课程级别的完成度计算。
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(Long userId, List<Long> lessonIds);

    long countByUserIdAndLessonIdInAndCompletedTrue(Long userId, List<Long> lessonIds);

    /**
     * 查询用户在指定课程下的所有课时进度。
     * 通过 lesson -> chapter -> course 关联查询。
     */
    @Query("SELECT lp FROM LessonProgress lp " +
           "JOIN FETCH lp.lesson l " +
           "WHERE lp.userId = :userId AND l.chapter.course.id = :courseId")
    List<LessonProgress> findByUserIdAndCourseId(@Param("userId") Long userId,
                                                  @Param("courseId") Long courseId);

    /**
     * 统计用户在指定课程下已完成的课时数。
     */
    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
           "WHERE lp.userId = :userId AND lp.lesson.chapter.course.id = :courseId AND lp.completed = true")
    long countCompletedByUserIdAndCourseId(@Param("userId") Long userId,
                                            @Param("courseId") Long courseId);
}
