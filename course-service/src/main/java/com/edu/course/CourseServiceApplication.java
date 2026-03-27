package com.edu.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 课程服务启动类。
 * 启用 Feign 客户端以支持跨服务调用用户服务获取讲师信息。
 */
@SpringBootApplication
@EnableFeignClients
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
