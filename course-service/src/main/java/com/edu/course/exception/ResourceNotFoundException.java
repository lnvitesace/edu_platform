package com.edu.course.exception;

import lombok.Getter;

/**
 * 资源未找到异常，映射 HTTP 404。
 * 携带资源类型、查询字段和查询值信息，便于生成友好的错误消息。
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
