package com.edu.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 搜索服务启动类。
 * 基于 Elasticsearch 提供课程全文检索能力，通过 RabbitMQ 异步同步课程数据。
 */
@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
