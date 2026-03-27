package com.edu.course.service;

import com.edu.course.dto.CategoryRequest;
import com.edu.course.dto.CategoryResponse;
import com.edu.course.entity.Category;
import com.edu.course.entity.Course;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private Category childCategory;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Programming")
                .parentId(null)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .courses(new ArrayList<>())
                .build();

        childCategory = Category.builder()
                .id(2L)
                .name("Java")
                .parentId(1L)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .courses(new ArrayList<>())
                .build();

        categoryRequest = CategoryRequest.builder()
                .name("Programming")
                .parentId(null)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("getAllCategories - Returns all root categories with children")
    void getAllCategories_Success() {
        when(categoryRepository.findByParentIdIsNullOrderBySortOrderAsc())
                .thenReturn(List.of(testCategory));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(childCategory));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(2L))
                .thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Programming");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("getCategoryById - Returns category successfully")
    void getCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(1L))
                .thenReturn(Collections.emptyList());

        CategoryResponse result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Programming");
    }

    @Test
    @DisplayName("getCategoryById - Throws exception when not found")
    void getCategoryById_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("createCategory - Creates category successfully")
    void createCategory_Success() {
        when(categoryRepository.existsByName("Programming")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        CategoryResponse result = categoryService.createCategory(categoryRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Programming");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory - Creates category with parent")
    void createCategory_WithParent() {
        categoryRequest.setParentId(1L);
        when(categoryRepository.existsByName("Programming")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);

        CategoryResponse result = categoryService.createCategory(categoryRequest);

        assertThat(result).isNotNull();
        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("createCategory - Throws exception when name exists")
    void createCategory_NameExists() {
        when(categoryRepository.existsByName("Programming")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createCategory - Throws exception when parent not found")
    void createCategory_ParentNotFound() {
        categoryRequest.setParentId(999L);
        when(categoryRepository.existsByName("Programming")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent Category");
    }

    @Test
    @DisplayName("updateCategory - Updates category successfully")
    void updateCategory_Success() {
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Updated Name")
                .sortOrder(2)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameAndIdNot("Updated Name", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        CategoryResponse result = categoryService.updateCategory(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory - Throws exception when category not found")
    void updateCategory_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, categoryRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateCategory - Throws exception when name already exists")
    void updateCategory_NameExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameAndIdNot("Programming", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, categoryRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateCategory - Throws exception when setting self as parent")
    void updateCategory_SelfParent() {
        categoryRequest.setParentId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameAndIdNot("Programming", 1L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, categoryRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    @DisplayName("deleteCategory - Deletes category successfully")
    void deleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(1L))
                .thenReturn(Collections.emptyList());

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(testCategory);
    }

    @Test
    @DisplayName("deleteCategory - Throws exception when has children")
    void deleteCategory_HasChildren() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(childCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("child categories");
    }

    @Test
    @DisplayName("deleteCategory - Throws exception when has courses")
    void deleteCategory_HasCourses() {
        Category categoryWithCourse = Category.builder()
                .id(3L)
                .name("Data Science")
                .courses(new ArrayList<>(List.of(Course.builder().id(99L).build())))
                .build();

        when(categoryRepository.findById(3L)).thenReturn(Optional.of(categoryWithCourse));
        when(categoryRepository.findByParentIdOrderBySortOrderAsc(3L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> categoryService.deleteCategory(3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("associated courses");
    }

    @Test
    @DisplayName("deleteCategory - Throws exception when not found")
    void deleteCategory_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createCategory - Uses default sortOrder when null")
    void createCategory_DefaultSortOrder() {
        categoryRequest.setSortOrder(null);
        when(categoryRepository.existsByName("Programming")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            assertThat(saved.getSortOrder()).isEqualTo(0);
            return testCategory;
        });

        categoryService.createCategory(categoryRequest);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory - Updates parent successfully")
    void updateCategory_WithNewParent() {
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Updated Name")
                .parentId(2L)
                .build();

        Category newParent = Category.builder().id(2L).name("Parent").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameAndIdNot("Updated Name", 1L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newParent));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        categoryService.updateCategory(1L, updateRequest);

        verify(categoryRepository).findById(2L);
        verify(categoryRepository).save(any(Category.class));
    }
}
