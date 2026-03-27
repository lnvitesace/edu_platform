package com.edu.course.exception;

/**
 * 错误请求异常，映射 HTTP 400。
 * 用于业务规则校验失败的场景（如删除有子节点的分类）。
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
