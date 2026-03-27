package com.edu.gateway.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 手动创建 Nacos NamingService Bean。
 * <p>
 * 未使用 spring-cloud-starter-alibaba-nacos-discovery 的自动配置，
 * 因为 Spring Cloud Gateway 基于 WebFlux，与 Nacos starter 的部分 Servlet
 * 依赖冲突。这里直接通过 NacosFactory 创建 NamingService 来规避此问题。
 */
@Slf4j
@Configuration
public class NacosConfig {

    @Value("${nacos.discovery.server-addr}")
    private String serverAddr;

    @Value("${nacos.discovery.namespace:public}")
    private String namespace;

    @Bean
    public NamingService namingService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (namespace != null && !namespace.isEmpty()) {
            properties.setProperty("namespace", namespace);
        }
        log.info("Initializing Nacos NamingService with server: {}", serverAddr);
        return NacosFactory.createNamingService(properties);
    }
}
