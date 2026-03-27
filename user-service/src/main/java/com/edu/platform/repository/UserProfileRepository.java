package com.edu.platform.repository;

import com.edu.platform.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户资料数据访问仓库 - 提供用户资料实体的数据库操作接口
 *
 * <p>继承 {@link JpaRepository}，自动获得基本的 CRUD 操作方法。
 * 用户资料与用户实体是一对一关系，通过 userId 关联。</p>
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>根据用户 ID 查询用户的扩展资料</li>
 *   <li>用户资料包含：昵称、头像、简介、国家、城市等信息</li>
 *   <li>继承的 CRUD 方法：save、findById、findAll、delete 等</li>
 * </ul>
 *
 * @author EduPlatform
 * @since 1.0
 * @see UserProfile
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * 根据用户 ID 查找用户资料
     *
     * <p>通过用户主表的 ID 查询对应的扩展资料记录。
     * 用户与资料是一对一关系，每个用户最多有一条资料记录。</p>
     *
     * @param userId 用户 ID（关联 users 表的主键）
     * @return 包含用户资料的 Optional，用户无资料时返回空 Optional
     */
    Optional<UserProfile> findByUserId(Long userId);
}
