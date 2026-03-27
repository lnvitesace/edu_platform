package com.edu.platform.exception;

import lombok.Getter;

/**
 * 资源未找到异常 - 当请求的资源在系统中不存在时抛出
 *
 * <p>这是一个运行时异常，用于表示客户端请求的资源（如用户、课程等）
 * 在数据库中不存在的情况。该异常会被 {@link GlobalExceptionHandler} 捕获
 * 并转换为 HTTP 404 Not Found 响应。</p>
 *
 * <h3>触发场景:</h3>
 * <ul>
 *   <li>根据 ID 查询用户，但用户不存在</li>
 *   <li>根据用户名或邮箱查询用户，但未找到匹配记录</li>
 *   <li>刷新令牌在数据库中不存在</li>
 *   <li>其他任何需要查询但未找到资源的场景</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * User user = userRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
 * }</pre>
 *
 * @author EduPlatform
 * @since 1.0
 * @see GlobalExceptionHandler#handleResourceNotFoundException(ResourceNotFoundException)
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    /** 资源名称，如 "User"、"Course" 等 */
    private String resourceName;

    /** 查询字段名称，如 "id"、"username"、"email" 等 */
    private String fieldName;

    /** 查询字段的值，即客户端提供的查询条件 */
    private Object fieldValue;

    /**
     * 构造资源未找到异常
     *
     * <p>生成格式化的错误消息："{资源名} not found with {字段名} : '{字段值}'"</p>
     *
     * @param resourceName 资源名称（如 "User"、"Course"）
     * @param fieldName 查询字段名（如 "id"、"username"）
     * @param fieldValue 查询字段值（如用户 ID、用户名）
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // 调用父类构造函数，生成格式化的错误消息
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
