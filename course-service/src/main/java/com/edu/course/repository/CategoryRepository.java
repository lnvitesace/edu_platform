package com.edu.course.repository;

import com.edu.course.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分类数据访问层。
 *
 * 查询方法支持树形结构遍历：先查根节点，再递归查子节点。
 * existsByName 系列方法用于创建/更新时的唯一性校验。
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIdIsNullOrderBySortOrderAsc();

    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
