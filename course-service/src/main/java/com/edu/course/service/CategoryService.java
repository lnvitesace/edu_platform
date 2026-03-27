package com.edu.course.service;

import com.edu.course.dto.CategoryRequest;
import com.edu.course.dto.CategoryResponse;
import com.edu.course.entity.Category;
import com.edu.course.exception.BadRequestException;
import com.edu.course.exception.ResourceNotFoundException;
import com.edu.course.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类业务逻辑层。
 *
 * 分类数据相对稳定，采用激进的缓存策略（24 小时 TTL）。
 * 任何分类的增删改都会清空整个分类缓存（allEntries=true），
 * 因为树形结构变更可能影响多个节点。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories:all")
    public List<CategoryResponse> getAllCategories() {
        List<Category> rootCategories = categoryRepository.findByParentIdIsNullOrderBySortOrderAsc();
        return rootCategories.stream()
                .map(this::mapToCategoryResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToCategoryResponseWithChildren(category);
    }

    @Transactional
    @CacheEvict(value = "categories:all", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        if (request.getParentId() != null) {
            categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories:all", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", request.getParentId()));
        }

        category.setName(request.getName());
        category.setParentId(request.getParentId());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    /**
     * 删除分类。
     * 安全限制：
     * 1. 有子分类的分类不能删除（保护树形结构完整性）
     * 2. 关联了课程的分类不能删除（保护数据关联完整性）
     */
    @Transactional
    @CacheEvict(value = "categories:all", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        List<Category> children = categoryRepository.findByParentIdOrderBySortOrderAsc(id);
        if (!children.isEmpty()) {
            throw new BadRequestException("Cannot delete category with child categories");
        }

        if (!category.getCourses().isEmpty()) {
            throw new BadRequestException("Cannot delete category with associated courses");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * 递归构建分类树。
     * 注意：对于深层级或大量分类，此实现会产生 N+1 查询问题，
     * 可考虑改为一次性加载所有分类后在内存中构建树。
     */
    private CategoryResponse mapToCategoryResponseWithChildren(Category category) {
        List<Category> children = categoryRepository.findByParentIdOrderBySortOrderAsc(category.getId());
        List<CategoryResponse> childResponses = children.stream()
                .map(this::mapToCategoryResponseWithChildren)
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .children(childResponses.isEmpty() ? null : childResponses)
                .build();
    }
}
