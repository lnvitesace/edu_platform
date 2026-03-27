package com.edu.course.repository;

import com.edu.course.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 报名数据访问层。
 *
 * 自定义查询说明：
 * - findActiveEnrollmentsByUserId: 使用 JOIN FETCH 避免懒加载导致的 N+1 查询
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.userId = :userId AND e.status = 'ACTIVE' ORDER BY e.enrolledAt DESC")
    List<Enrollment> findActiveEnrollmentsByUserId(@Param("userId") Long userId);

    long countByCourseId(Long courseId);
}
