package com.edu.course.service;

import com.edu.course.client.UserServiceClient;
import com.edu.course.client.dto.UserResponse;
import com.edu.course.dto.*;
import com.edu.course.entity.Category;
import com.edu.course.entity.Course;
import com.edu.course.event.CourseEventPublisher;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.CategoryRepository;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程业务逻辑层。
 *
 * 核心职责：
 * 1. 课程生命周期管理（创建、发布、归档、删除）
 * 2. 权限控制（仅课程创建者或管理员可修改）
 * 3. 缓存管理（课程详情缓存，写操作时失效）
 * 4. 事件发布（课程变更通知搜索服务更新索引）
 *
 * 事务策略：
 * - 读操作使用 readOnly=true 优化数据库连接
 * - 写操作确保原子性，失败时完整回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;
    private final UserServiceClient userServiceClient;
    private final CourseEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCourses(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Course> coursePage = courseRepository.findByStatus(Course.CourseStatus.PUBLISHED, pageable);
        return mapToPageResponse(coursePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCoursesByCategory(Long categoryId, int page, int size) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseRepository.findByCategoryId(categoryId, pageable);
        return mapToPageResponse(coursePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCoursesByInstructor(Long instructorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseRepository.findByInstructorId(instructorId, pageable);
        return mapToPageResponse(coursePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> searchCourses(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseRepository.searchByKeyword(keyword, Course.CourseStatus.PUBLISHED, pageable);
        return mapToPageResponse(coursePage);
    }

    /**
     * 缓存课程详情，包含章节和课时的完整结构。
     * 缓存 key 为课程 ID，TTL 由 RedisConfig 配置（60 分钟）。
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "courses:detail", key = "#id")
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
        List<com.edu.course.entity.Chapter> chapters = chapterRepository.findByCourseIdWithLessons(id);
        return mapToCourseResponseWithDetails(course, chapters);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request, Long instructorId) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .price(request.getPrice())
                .instructorId(instructorId)
                .category(category)
                .status(Course.CourseStatus.DRAFT)
                .build();

        course = courseRepository.save(course);
        eventPublisher.publishCourseCreated(course);
        return mapToCourseResponse(course);
    }

    /**
     * 更新课程信息。
     * CacheEvict 确保缓存与数据库一致。
     * 仅已发布课程的更新会触发事件通知搜索服务。
     */
    @Transactional
    @CacheEvict(value = "courses:detail", key = "#id")
    public CourseResponse updateCourse(Long id, CourseRequest request, Long userId, String userRole) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        checkCourseOwnership(course, userId, userRole);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCoverImage(request.getCoverImage());
        course.setPrice(request.getPrice());
        course.setCategory(category);

        course = courseRepository.save(course);
        if (course.getStatus() == Course.CourseStatus.PUBLISHED) {
            eventPublisher.publishCourseUpdated(course);
        }
        return mapToCourseResponse(course);
    }

    @Transactional
    @CacheEvict(value = "courses:detail", key = "#id")
    public CourseResponse publishCourse(Long id, Long userId, String userRole) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        checkCourseOwnership(course, userId, userRole);

        if (course.getStatus() == Course.CourseStatus.PUBLISHED) {
            throw new BadRequestException("Course is already published");
        }

        course.setStatus(Course.CourseStatus.PUBLISHED);
        course = courseRepository.save(course);
        eventPublisher.publishCoursePublished(course);
        return mapToCourseResponse(course);
    }

    @Transactional
    @CacheEvict(value = "courses:detail", key = "#id")
    public CourseResponse archiveCourse(Long id, Long userId, String userRole) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        checkCourseOwnership(course, userId, userRole);

        course.setStatus(Course.CourseStatus.ARCHIVED);
        course = courseRepository.save(course);
        eventPublisher.publishCourseDeleted(course.getId());
        return mapToCourseResponse(course);
    }

    /**
     * 删除课程。
     * 安全限制：已发布课程不能直接删除，必须先归档。
     * 这是为了防止误删导致学员已购课程消失。
     */
    @Transactional
    @CacheEvict(value = "courses:detail", key = "#id")
    public void deleteCourse(Long id, Long userId, String userRole) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        checkCourseOwnership(course, userId, userRole);

        if (course.getStatus() == Course.CourseStatus.PUBLISHED) {
            throw new BadRequestException("Cannot delete a published course. Archive it first.");
        }

        courseRepository.delete(course);
        eventPublisher.publishCourseDeleted(id);
    }

    /**
     * 权限检查：仅允许课程创建者或管理员修改课程。
     * ADMIN 角色可管理所有课程，用于内容审核和违规处理。
     */
    private void checkCourseOwnership(Course course, Long userId, String userRole) {
        if (!"ADMIN".equals(userRole) && !course.getInstructorId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to modify this course");
        }
    }

    private PageResponse<CourseResponse> mapToPageResponse(Page<Course> coursePage) {
        List<CourseResponse> content = coursePage.getContent().stream()
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());

        return PageResponse.<CourseResponse>builder()
                .content(content)
                .pageNumber(coursePage.getNumber())
                .pageSize(coursePage.getSize())
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .first(coursePage.isFirst())
                .last(coursePage.isLast())
                .build();
    }

    private CourseResponse mapToCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .coverImage(course.getCoverImage())
                .price(course.getPrice())
                .instructorId(course.getInstructorId())
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .status(course.getStatus().name())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private CourseResponse mapToCourseResponseWithDetails(Course course, List<com.edu.course.entity.Chapter> chapters) {
        List<ChapterResponse> chapterResponses = chapters.stream()
                .map(chapter -> ChapterResponse.builder()
                        .id(chapter.getId())
                        .courseId(course.getId())
                        .title(chapter.getTitle())
                        .sortOrder(chapter.getSortOrder())
                        .createdAt(chapter.getCreatedAt())
                        .updatedAt(chapter.getUpdatedAt())
                        .lessons(chapter.getLessons() != null
                                ? chapter.getLessons().stream()
                                    .map(lesson -> LessonResponse.builder()
                                            .id(lesson.getId())
                                            .chapterId(chapter.getId())
                                            .title(lesson.getTitle())
                                            .videoUrl(lesson.getVideoUrl())
                                            .duration(lesson.getDuration())
                                            .isFree(lesson.getIsFree())
                                            .sortOrder(lesson.getSortOrder())
                                            .createdAt(lesson.getCreatedAt())
                                            .updatedAt(lesson.getUpdatedAt())
                                            .build())
                                    .collect(Collectors.toList())
                                : Collections.emptyList())
                        .build())
                .collect(Collectors.toList());

        CourseResponse.InstructorInfo instructorInfo = fetchInstructorInfo(course.getInstructorId());

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .coverImage(course.getCoverImage())
                .price(course.getPrice())
                .instructorId(course.getInstructorId())
                .instructor(instructorInfo)
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .status(course.getStatus().name())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .chapters(chapterResponses)
                .build();
    }

    /**
     * 通过 Feign 调用 user-service 获取讲师信息。
     * 如果调用失败，fallback 返回占位信息，不影响课程数据展示。
     */
    private CourseResponse.InstructorInfo fetchInstructorInfo(Long instructorId) {
        if (instructorId == null) {
            return null;
        }
        try {
            UserResponse user = userServiceClient.getUserById(instructorId);
            return CourseResponse.InstructorInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch instructor info for id {}: {}", instructorId, e.getMessage());
            return CourseResponse.InstructorInfo.builder()
                    .id(instructorId)
                    .username("Unknown")
                    .nickname("Unknown Instructor")
                    .build();
        }
    }
}
