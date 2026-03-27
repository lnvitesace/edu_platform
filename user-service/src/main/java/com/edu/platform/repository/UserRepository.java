package com.edu.platform.repository;

import com.edu.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问仓库 - 提供用户实体的数据库操作接口
 *
 * <p>继承 {@link JpaRepository}，自动获得基本的 CRUD 操作方法。
 * 同时定义了多个自定义查询方法，用于用户认证和唯一性校验。</p>
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>用户登录时根据用户名或邮箱查找用户</li>
 *   <li>用户注册时校验用户名和邮箱的唯一性</li>
 *   <li>继承的 CRUD 方法：save、findById、findAll、delete 等</li>
 * </ul>
 *
 * @author EduPlatform
 * @since 1.0
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     *
     * <p>用于用户登录验证和用户名查重。用户名在系统中是唯一的。</p>
     *
     * @param username 用户名
     * @return 包含用户实体的 Optional，未找到时返回空 Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     *
     * <p>用于用户登录验证和邮箱查重。邮箱在系统中是唯一的。</p>
     *
     * @param email 用户邮箱地址
     * @return 包含用户实体的 Optional，未找到时返回空 Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     *
     * <p>用于用户登录时，允许用户使用用户名或邮箱进行登录。
     * 两个参数使用 OR 条件匹配，任一匹配即返回用户。</p>
     *
     * @param username 用户名
     * @param email 邮箱地址
     * @return 包含用户实体的 Optional，未找到时返回空 Optional
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 检查用户名是否已存在
     *
     * <p>用于用户注册时快速校验用户名的唯一性，
     * 避免在保存时触发数据库唯一约束异常。</p>
     *
     * @param username 要检查的用户名
     * @return true 表示用户名已存在，false 表示可用
     */
    Boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     *
     * <p>用于用户注册时快速校验邮箱的唯一性，
     * 避免在保存时触发数据库唯一约束异常。</p>
     *
     * @param email 要检查的邮箱地址
     * @return true 表示邮箱已存在，false 表示可用
     */
    Boolean existsByEmail(String email);
}
