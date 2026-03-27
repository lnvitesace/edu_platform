package com.edu.platform.exception;

/**
 * 错误请求异常 - 当客户端请求参数无效或业务规则不满足时抛出
 *
 * <p>这是一个运行时异常，用于表示客户端的请求存在问题，
 * 如参数格式错误、业务逻辑冲突等。该异常会被 {@link GlobalExceptionHandler}
 * 捕获并转换为 HTTP 400 Bad Request 响应。</p>
 *
 * <h3>触发场景:</h3>
 * <ul>
 *   <li>用户注册时，用户名已被占用</li>
 *   <li>用户注册时，邮箱已被注册</li>
 *   <li>密码不符合安全策略要求</li>
 *   <li>请求参数不符合业务规则</li>
 *   <li>操作违反数据完整性约束</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * if (userRepository.existsByUsername(username)) {
 *     throw new BadRequestException("Username is already taken");
 * }
 * }</pre>
 *
 * @author EduPlatform
 * @since 1.0
 * @see GlobalExceptionHandler#handleBadRequestException(BadRequestException)
 */
public class BadRequestException extends RuntimeException {

    /**
     * 构造错误请求异常
     *
     * @param message 描述错误原因的详细消息，将返回给客户端
     */
    public BadRequestException(String message) {
        super(message);
    }
}
