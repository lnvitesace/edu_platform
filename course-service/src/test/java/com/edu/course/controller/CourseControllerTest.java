package com.edu.course.controller;

import com.edu.course.dto.CourseRequest;
import com.edu.course.dto.CourseResponse;
import com.edu.course.dto.PageResponse;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.GlobalExceptionHandler;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseController Unit Tests")
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private ObjectMapper objectMapper;
    private CourseResponse testCourseResponse;
    private CourseRequest courseRequest;
    private PageResponse<CourseResponse> pageResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testCourseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .description("Learn Java")
                .instructorId(100L)
                .categoryId(1L)
                .categoryName("Programming")
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        courseRequest = CourseRequest.builder()
                .title("Java Basics")
                .description("Learn Java")
                .categoryId(1L)
                .build();

        pageResponse = PageResponse.<CourseResponse>builder()
                .content(List.of(testCourseResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/courses - Returns paginated courses")
    void getAllCourses_Success() throws Exception {
        when(courseService.getAllCourses(0, 10, "createdAt", "desc")).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"));
    }

    @Test
    @DisplayName("GET /api/courses/category/{categoryId} - Returns courses by category")
    void getCoursesByCategory_Success() throws Exception {
        when(courseService.getCoursesByCategory(1L, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(1));
    }

    @Test
    @DisplayName("GET /api/courses/instructor/{instructorId} - Returns courses by instructor")
    void getCoursesByInstructor_Success() throws Exception {
        when(courseService.getCoursesByInstructor(100L, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses/instructor/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].instructorId").value(100));
    }

    @Test
    @DisplayName("GET /api/courses/search - Returns matching courses")
    void searchCourses_Success() throws Exception {
        when(courseService.searchCourses("Java", 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses/search?keyword=Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"));
    }

    @Test
    @DisplayName("GET /api/courses/{id} - Returns course by id")
    void getCourseById_Success() throws Exception {
        testCourseResponse.setChapters(Collections.emptyList());
        when(courseService.getCourseById(1L)).thenReturn(testCourseResponse);

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    @DisplayName("GET /api/courses/{id} - Returns 404 when not found")
    void getCourseById_NotFound() throws Exception {
        when(courseService.getCourseById(999L))
                .thenThrow(new ResourceNotFoundException("Course", "id", 999L));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/courses - Creates course")
    void createCourse_Success() throws Exception {
        when(courseService.createCourse(any(CourseRequest.class), eq(100L)))
                .thenReturn(testCourseResponse);

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    @DisplayName("POST /api/courses - Returns 400 for invalid request")
    void createCourse_InvalidRequest() throws Exception {
        CourseRequest invalidRequest = CourseRequest.builder().build();

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/courses/{id} - Updates course")
    void updateCourse_Success() throws Exception {
        when(courseService.updateCourse(eq(1L), any(CourseRequest.class), eq(100L), eq("INSTRUCTOR")))
                .thenReturn(testCourseResponse);

        mockMvc.perform(put("/api/courses/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    @DisplayName("PUT /api/courses/{id} - Returns 403 when not owner")
    void updateCourse_Forbidden() throws Exception {
        when(courseService.updateCourse(eq(1L), any(CourseRequest.class), eq(999L), eq("INSTRUCTOR")))
                .thenThrow(new ForbiddenException("Not authorized"));

        mockMvc.perform(put("/api/courses/1")
                        .header("X-User-Id", "999")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/courses/{id}/publish - Publishes course")
    void publishCourse_Success() throws Exception {
        testCourseResponse.setStatus("PUBLISHED");
        when(courseService.publishCourse(1L, 100L, "INSTRUCTOR")).thenReturn(testCourseResponse);

        mockMvc.perform(put("/api/courses/1/publish")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("PUT /api/courses/{id}/archive - Archives course")
    void archiveCourse_Success() throws Exception {
        testCourseResponse.setStatus("ARCHIVED");
        when(courseService.archiveCourse(1L, 100L, "INSTRUCTOR")).thenReturn(testCourseResponse);

        mockMvc.perform(put("/api/courses/1/archive")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("DELETE /api/courses/{id} - Deletes course")
    void deleteCourse_Success() throws Exception {
        doNothing().when(courseService).deleteCourse(1L, 100L, "INSTRUCTOR");

        mockMvc.perform(delete("/api/courses/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isNoContent());
    }
}
