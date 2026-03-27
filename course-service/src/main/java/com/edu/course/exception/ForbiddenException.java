package com.edu.course.exception;

/**
 * 权限不足异常，映射 HTTP 403。
 * 用于用户已认证但无权操作目标资源的场景。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
