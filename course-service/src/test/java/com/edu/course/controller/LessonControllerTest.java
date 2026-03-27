package com.edu.course.controller;

import com.edu.course.dto.LessonRequest;
import com.edu.course.dto.LessonResponse;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.GlobalExceptionHandler;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.service.LessonService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonController Unit Tests")
class LessonControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private LessonController lessonController;

    private ObjectMapper objectMapper;
    private LessonResponse testLessonResponse;
    private LessonRequest lessonRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testLessonResponse = LessonResponse.builder()
                .id(1L)
                .chapterId(1L)
                .title("Hello World")
                .videoUrl("http://example.com/video.mp4")
                .duration(600)
                .isFree(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lessonRequest = LessonRequest.builder()
                .title("Hello World")
                .videoUrl("http://example.com/video.mp4")
                .duration(600)
                .isFree(true)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("GET /api/courses/chapters/{chapterId}/lessons - Returns lessons")
    void getLessonsByChapterId_Success() throws Exception {
        when(lessonService.getLessonsByChapterId(1L)).thenReturn(List.of(testLessonResponse));

        mockMvc.perform(get("/api/courses/chapters/1/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Hello World"));
    }

    @Test
    @DisplayName("GET /api/courses/lessons/{id} - Returns lesson by id")
    void getLessonById_Success() throws Exception {
        when(lessonService.getLessonById(1L)).thenReturn(testLessonResponse);

        mockMvc.perform(get("/api/courses/lessons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello World"));
    }

    @Test
    @DisplayName("GET /api/courses/lessons/{id} - Returns 404 when not found")
    void getLessonById_NotFound() throws Exception {
        when(lessonService.getLessonById(999L))
                .thenThrow(new ResourceNotFoundException("Lesson", "id", 999L));

        mockMvc.perform(get("/api/courses/lessons/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/courses/chapters/{chapterId}/lessons - Creates lesson")
    void createLesson_Success() throws Exception {
        when(lessonService.createLesson(eq(1L), any(LessonRequest.class), eq(100L), eq("INSTRUCTOR")))
                .thenReturn(testLessonResponse);

        mockMvc.perform(post("/api/courses/chapters/1/lessons")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Hello World"));
    }

    @Test
    @DisplayName("POST /api/courses/chapters/{chapterId}/lessons - Returns 403 when not owner")
    void createLesson_Forbidden() throws Exception {
        when(lessonService.createLesson(eq(1L), any(LessonRequest.class), eq(999L), eq("INSTRUCTOR")))
                .thenThrow(new ForbiddenException("Not authorized"));

        mockMvc.perform(post("/api/courses/chapters/1/lessons")
                        .header("X-User-Id", "999")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/courses/lessons/{id} - Updates lesson")
    void updateLesson_Success() throws Exception {
        when(lessonService.updateLesson(eq(1L), any(LessonRequest.class), eq(100L), eq("INSTRUCTOR")))
                .thenReturn(testLessonResponse);

        mockMvc.perform(put("/api/courses/lessons/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello World"));
    }

    @Test
    @DisplayName("DELETE /api/courses/lessons/{id} - Deletes lesson")
    void deleteLesson_Success() throws Exception {
        doNothing().when(lessonService).deleteLesson(1L, 100L, "INSTRUCTOR");

        mockMvc.perform(delete("/api/courses/lessons/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/courses/chapters/{chapterId}/lessons - Returns 400 for invalid request")
    void createLesson_InvalidRequest() throws Exception {
        LessonRequest invalidRequest = LessonRequest.builder().build();

        mockMvc.perform(post("/api/courses/chapters/1/lessons")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
