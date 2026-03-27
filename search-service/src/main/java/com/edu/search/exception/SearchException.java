package com.edu.search.exception;

/**
 * 搜索操作异常，封装 Elasticsearch 相关错误。
 */
public class SearchException extends RuntimeException {

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
