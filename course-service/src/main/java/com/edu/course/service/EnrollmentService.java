package com.edu.course.service;

import com.edu.course.dto.EnrollmentRequest;
import com.edu.course.dto.EnrollmentResponse;
import com.edu.course.entity.Course;
import com.edu.course.entity.Enrollment;
import com.edu.course.entity.Lesson;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.CourseRepository;
import com.edu.course.repository.EnrollmentRepository;
import com.edu.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 报名业务逻辑层。
 *
 * 处理用户报名、取消报名、检查报名状态等操作。
 * checkLessonAccess 用于在访问课时前验证用户是否已报名。
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public EnrollmentResponse enroll(EnrollmentRequest request, Long userId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        if (course.getStatus() != Course.CourseStatus.PUBLISHED) {
            throw new BadRequestException("Can only enroll in published courses");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, request.getCourseId())) {
            throw new BadRequestException("Already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .course(course)
                .build();

        enrollment = enrollmentRepository.save(enrollment);
        return mapToResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(Long userId) {
        return enrollmentRepository.findActiveEnrollmentsByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelEnrollment(Long courseId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "courseId", courseId));

        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public void checkLessonAccess(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        if (Boolean.TRUE.equals(lesson.getIsFree())) {
            return;
        }

        // Non-free lessons are not accessible (no payment system available)
        throw new ForbiddenException("This lesson requires a paid subscription");
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseCoverImage(course.getCoverImage())
                .instructorId(course.getInstructorId())
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
