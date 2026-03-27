package com.edu.course.controller;

import com.edu.course.dto.ChapterRequest;
import com.edu.course.dto.ChapterResponse;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.GlobalExceptionHandler;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.service.ChapterService;
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
@DisplayName("ChapterController Unit Tests")
class ChapterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChapterService chapterService;

    @InjectMocks
    private ChapterController chapterController;

    private ObjectMapper objectMapper;
    private ChapterResponse testChapterResponse;
    private ChapterRequest chapterRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chapterController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testChapterResponse = ChapterResponse.builder()
                .id(1L)
                .courseId(1L)
                .title("Introduction")
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lessons(Collections.emptyList())
                .build();

        chapterRequest = ChapterRequest.builder()
                .title("Introduction")
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("GET /api/courses/{courseId}/chapters - Returns chapters")
    void getChaptersByCourseId_Success() throws Exception {
        when(chapterService.getChaptersByCourseId(1L)).thenReturn(List.of(testChapterResponse));

        mockMvc.perform(get("/api/courses/1/chapters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Introduction"));
    }

    @Test
    @DisplayName("GET /api/courses/chapters/{id} - Returns chapter by id")
    void getChapterById_Success() throws Exception {
        when(chapterService.getChapterById(1L)).thenReturn(testChapterResponse);

        mockMvc.perform(get("/api/courses/chapters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Introduction"));
    }

    @Test
    @DisplayName("GET /api/courses/chapters/{id} - Returns 404 when not found")
    void getChapterById_NotFound() throws Exception {
        when(chapterService.getChapterById(999L))
                .thenThrow(new ResourceNotFoundException("Chapter", "id", 999L));

        mockMvc.perform(get("/api/courses/chapters/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/courses/{courseId}/chapters - Creates chapter")
    void createChapter_Success() throws Exception {
        when(chapterService.createChapter(eq(1L), any(ChapterRequest.class), eq(100L), eq("INSTRUCTOR")))
                .thenReturn(testChapterResponse);

        mockMvc.perform(post("/api/courses/1/chapters")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chapterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Introduction"));
    }

    @Test
    @DisplayName("POST /api/courses/{courseId}/chapters - Returns 403 when not owner")
    void createChapter_Forbidden() throws Exception {
        when(chapterService.createChapter(eq(1L), any(ChapterRequest.class), eq(999L), eq("INSTRUCTOR")))
                .thenThrow(new ForbiddenException("Not authorized"));

        mockMvc.perform(post("/api/courses/1/chapters")
                        .header("X-User-Id", "999")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chapterRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/courses/chapters/{id} - Updates chapter")
    void updateChapter_Success() throws Exception {
        when(chapterService.updateChapter(eq(1L), any(ChapterRequest.class), eq(100L), eq("INSTRUCTOR")))
                .thenReturn(testChapterResponse);

        mockMvc.perform(put("/api/courses/chapters/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chapterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Introduction"));
    }

    @Test
    @DisplayName("DELETE /api/courses/chapters/{id} - Deletes chapter")
    void deleteChapter_Success() throws Exception {
        doNothing().when(chapterService).deleteChapter(1L, 100L, "INSTRUCTOR");

        mockMvc.perform(delete("/api/courses/chapters/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/courses/{courseId}/chapters - Returns 400 for invalid request")
    void createChapter_InvalidRequest() throws Exception {
        ChapterRequest invalidRequest = ChapterRequest.builder().build();

        mockMvc.perform(post("/api/courses/1/chapters")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
