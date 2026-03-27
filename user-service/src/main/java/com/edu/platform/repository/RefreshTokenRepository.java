package com.edu.platform.repository;

import com.edu.platform.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 刷新令牌数据访问仓库 - 提供刷新令牌实体的数据库操作接口
 *
 * <p>继承 {@link JpaRepository}，自动获得基本的 CRUD 操作方法。
 * 刷新令牌用于在访问令牌过期后获取新的访问令牌，有效期为 7 天。</p>
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>根据令牌字符串查找刷新令牌记录</li>
 *   <li>用户登出时删除该用户的所有刷新令牌</li>
 *   <li>定期清理过期的刷新令牌</li>
 *   <li>继承的 CRUD 方法：save、findById、findAll、delete 等</li>
 * </ul>
 *
 * @author EduPlatform
 * @since 1.0
 * @see RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 根据令牌字符串查找刷新令牌
     *
     * <p>用于 Token 刷新接口，验证客户端提交的刷新令牌是否有效。
     * 找到记录后还需检查是否过期。</p>
     *
     * @param token 刷新令牌字符串（UUID 格式）
     * @return 包含刷新令牌实体的 Optional，未找到时返回空 Optional
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 删除指定用户的所有刷新令牌
     *
     * <p>用于用户登出时清除该用户的所有会话，确保之前颁发的
     * 刷新令牌全部失效。需要在事务中调用。</p>
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 删除指定时间之前过期的所有刷新令牌
     *
     * <p>用于定期清理任务，删除已过期的刷新令牌记录，
     * 避免数据库中积累大量无效数据。需要在事务中调用。</p>
     *
     * @param now 当前时间，删除 expiresAt 早于此时间的记录
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
