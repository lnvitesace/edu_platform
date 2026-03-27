package com.edu.course.service;

import com.edu.course.client.UserServiceClient;
import com.edu.course.client.dto.UserResponse;
import com.edu.course.dto.CourseRequest;
import com.edu.course.dto.CourseResponse;
import com.edu.course.dto.PageResponse;
import com.edu.course.entity.Category;
import com.edu.course.entity.Course;
import com.edu.course.event.CourseEventPublisher;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.CategoryRepository;
import com.edu.course.repository.ChapterRepository;
import com.edu.course.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CourseEventPublisher eventPublisher;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private Category testCategory;
    private CourseRequest courseRequest;
    private UserResponse testUser;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Programming")
                .build();

        testCourse = Course.builder()
                .id(1L)
                .title("Java Basics")
                .description("Learn Java from scratch")
                .coverImage("http://example.com/cover.jpg")
                .instructorId(100L)
                .category(testCategory)
                .status(Course.CourseStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .chapters(new ArrayList<>())
                .build();

        courseRequest = CourseRequest.builder()
                .title("Java Basics")
                .description("Learn Java from scratch")
                .coverImage("http://example.com/cover.jpg")
                .categoryId(1L)
                .build();

        testUser = UserResponse.builder()
                .id(100L)
                .username("instructor")
                .nickname("Test Instructor")
                .avatar("http://example.com/avatar.jpg")
                .build();
    }

    @Test
    @DisplayName("getAllCourses - Returns paginated courses")
    void getAllCourses_Success() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        when(courseRepository.findByStatus(eq(Course.CourseStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(coursePage);

        PageResponse<CourseResponse> result = courseService.getAllCourses(0, 10, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java Basics");
    }

    @Test
    @DisplayName("getAllCourses - Returns paginated courses with ascending sort")
    void getAllCourses_AscendingSort() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        when(courseRepository.findByStatus(eq(Course.CourseStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(coursePage);

        PageResponse<CourseResponse> result = courseService.getAllCourses(0, 10, "title", "asc");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getCoursesByCategory - Returns courses by category")
    void getCoursesByCategory_Success() {
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.findByCategoryId(eq(1L), any(Pageable.class))).thenReturn(coursePage);

        PageResponse<CourseResponse> result = courseService.getCoursesByCategory(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getCoursesByCategory - Throws exception when category not found")
    void getCoursesByCategory_CategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCoursesByCategory(999L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getCoursesByInstructor - Returns courses by instructor")
    void getCoursesByInstructor_Success() {
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        when(courseRepository.findByInstructorId(eq(100L), any(Pageable.class))).thenReturn(coursePage);

        PageResponse<CourseResponse> result = courseService.getCoursesByInstructor(100L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInstructorId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("searchCourses - Returns matching courses")
    void searchCourses_Success() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        when(courseRepository.searchByKeyword(eq("Java"), eq(Course.CourseStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(coursePage);

        PageResponse<CourseResponse> result = courseService.searchCourses("Java", 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getCourseById - Returns course with details and instructor info")
    void getCourseById_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.findByCourseIdWithLessons(1L)).thenReturn(new ArrayList<>());
        when(userServiceClient.getUserById(100L)).thenReturn(testUser);

        CourseResponse result = courseService.getCourseById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Java Basics");
        assertThat(result.getInstructor()).isNotNull();
        assertThat(result.getInstructor().getUsername()).isEqualTo("instructor");
    }

    @Test
    @DisplayName("getCourseById - Throws exception when not found")
    void getCourseById_NotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createCourse - Creates course and publishes event")
    void createCourse_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse result = courseService.createCourse(courseRequest, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Java Basics");
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        verify(eventPublisher).publishCourseCreated(any(Course.class));
    }

    @Test
    @DisplayName("createCourse - Creates course without category")
    void createCourse_WithoutCategory() {
        courseRequest.setCategoryId(null);
        Course courseWithoutCategory = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructorId(100L)
                .status(Course.CourseStatus.DRAFT)
                .build();
        when(courseRepository.save(any(Course.class))).thenReturn(courseWithoutCategory);

        CourseResponse result = courseService.createCourse(courseRequest, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    @DisplayName("createCourse - Throws exception when category not found")
    void createCourse_CategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(courseRequest, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateCourse - Updates course successfully by owner")
    void updateCourse_SuccessByOwner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse result = courseService.updateCourse(1L, courseRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("updateCourse - Throws exception when course not found")
    void updateCourse_CourseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(999L, courseRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateCourse - Throws exception when category not found")
    void updateCourse_CategoryNotFound() {
        courseRequest.setCategoryId(999L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(1L, courseRequest, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateCourse - Updates published course and publishes event")
    void updateCourse_PublishedCoursePublishesEvent() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        courseService.updateCourse(1L, courseRequest, 100L, "INSTRUCTOR");

        verify(eventPublisher).publishCourseUpdated(any(Course.class));
    }

    @Test
    @DisplayName("updateCourse - Updates course successfully by admin")
    void updateCourse_SuccessByAdmin() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse result = courseService.updateCourse(1L, courseRequest, 999L, "ADMIN");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateCourse - Throws forbidden when not owner")
    void updateCourse_ForbiddenNotOwner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> courseService.updateCourse(1L, courseRequest, 999L, "INSTRUCTOR"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("publishCourse - Publishes course and sends event")
    void publishCourse_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setStatus(Course.CourseStatus.PUBLISHED);
            return c;
        });

        CourseResponse result = courseService.publishCourse(1L, 100L, "INSTRUCTOR");

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        verify(eventPublisher).publishCoursePublished(any(Course.class));
    }

    @Test
    @DisplayName("publishCourse - Throws exception when course not found")
    void publishCourse_CourseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.publishCourse(999L, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("publishCourse - Throws exception when already published")
    void publishCourse_AlreadyPublished() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> courseService.publishCourse(1L, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already published");
    }

    @Test
    @DisplayName("archiveCourse - Throws exception when course not found")
    void archiveCourse_CourseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.archiveCourse(999L, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("archiveCourse - Archives course and sends delete event")
    void archiveCourse_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setStatus(Course.CourseStatus.ARCHIVED);
            return c;
        });

        CourseResponse result = courseService.archiveCourse(1L, 100L, "INSTRUCTOR");

        assertThat(result.getStatus()).isEqualTo("ARCHIVED");
        verify(eventPublisher).publishCourseDeleted(1L);
    }

    @Test
    @DisplayName("deleteCourse - Deletes draft course and sends event")
    void deleteCourse_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        courseService.deleteCourse(1L, 100L, "INSTRUCTOR");

        verify(courseRepository).delete(testCourse);
        verify(eventPublisher).publishCourseDeleted(1L);
    }

    @Test
    @DisplayName("deleteCourse - Throws exception when course not found")
    void deleteCourse_CourseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(999L, 100L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteCourse - Throws exception when published")
    void deleteCourse_CannotDeletePublished() {
        testCourse.setStatus(Course.CourseStatus.PUBLISHED);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> courseService.deleteCourse(1L, 100L, "INSTRUCTOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete a published course");
    }

    @Test
    @DisplayName("updateCourse - Updates without category")
    void updateCourse_WithoutCategory() {
        courseRequest.setCategoryId(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse result = courseService.updateCourse(1L, courseRequest, 100L, "INSTRUCTOR");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getCourseById - Handles null instructor gracefully")
    void getCourseById_NullInstructor() {
        testCourse.setInstructorId(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(chapterRepository.findByCourseIdWithLessons(1L)).thenReturn(new ArrayList<>());

        CourseResponse result = courseService.getCourseById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getInstructor()).isNull();
        verify(userServiceClient, never()).getUserById(anyLong());
    }
}
