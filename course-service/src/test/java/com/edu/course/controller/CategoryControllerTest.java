package com.edu.course.controller;

import com.edu.course.dto.CategoryRequest;
import com.edu.course.dto.CategoryResponse;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ForbiddenException;
import com.edu.course.exception.GlobalExceptionHandler;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.service.CategoryService;
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
@DisplayName("CategoryController Unit Tests")
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private ObjectMapper objectMapper;
    private CategoryResponse testCategoryResponse;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testCategoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Programming")
                .parentId(null)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        categoryRequest = CategoryRequest.builder()
                .name("Programming")
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("GET /api/courses/categories - Returns all categories")
    void getAllCategories_Success() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(testCategoryResponse));

        mockMvc.perform(get("/api/courses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Programming"));
    }

    @Test
    @DisplayName("GET /api/courses/categories/{id} - Returns category by id")
    void getCategoryById_Success() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse);

        mockMvc.perform(get("/api/courses/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Programming"));
    }

    @Test
    @DisplayName("GET /api/courses/categories/{id} - Returns 404 when not found")
    void getCategoryById_NotFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new ResourceNotFoundException("Category", "id", 999L));

        mockMvc.perform(get("/api/courses/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/courses/categories - Creates category")
    void createCategory_Success() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(testCategoryResponse);

        mockMvc.perform(post("/api/courses/categories")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Programming"));
    }

    @Test
    @DisplayName("POST /api/courses/categories - Returns 400 for invalid request")
    void createCategory_InvalidRequest() throws Exception {
        CategoryRequest invalidRequest = CategoryRequest.builder().build();

        mockMvc.perform(post("/api/courses/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/courses/categories - Returns 400 when name exists")
    void createCategory_NameExists() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new BadRequestException("Category already exists"));

        mockMvc.perform(post("/api/courses/categories")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/courses/categories/{id} - Updates category")
    void updateCategory_Success() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class)))
                .thenReturn(testCategoryResponse);

        mockMvc.perform(put("/api/courses/categories/1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Programming"));
    }

    @Test
    @DisplayName("DELETE /api/courses/categories/{id} - Deletes category")
    void deleteCategory_Success() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/courses/categories/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/courses/categories/{id} - Returns 400 when has children")
    void deleteCategory_HasChildren() throws Exception {
        doThrow(new BadRequestException("Cannot delete category with children"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/courses/categories/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/courses/categories - Returns 403 for non-admin user")
    void createCategory_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/courses/categories")
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }
}
