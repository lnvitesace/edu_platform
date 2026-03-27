package com.edu.search.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.net.InetAddress;
import java.util.Properties;

/**
 * Nacos 服务注册配置。
 * <p>
 * 未使用 Spring Cloud Nacos Discovery 自动注册，而是手动管理注册/注销生命周期，
 * 这样可以避免引入 spring-cloud-starter-alibaba 的传递依赖冲突。
 * matchIfMissing=true 确保未显式配置时默认启用服务注册。
 */
@Configuration
@ConditionalOnProperty(name = "nacos.discovery.enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfig {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfig.class);

    private static final int MAX_RETRY = 30;
    private static final long RETRY_INTERVAL_MS = 3000;

    @Value("${nacos.discovery.server-addr}")
    private String serverAddr;

    @Value("${nacos.discovery.namespace:public}")
    private String namespace;

    @Value("${nacos.discovery.group:DEFAULT_GROUP}")
    private String group;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private int port;

    @Value("${nacos.discovery.ip:#{null}}")
    private String configuredIp;

    private NamingService namingService;
    private String registeredIp;

    @Bean
    public NamingService namingService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        properties.setProperty("namespace", namespace);
        this.namingService = NacosFactory.createNamingService(properties);
        return this.namingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerToNacos() {
        this.registeredIp = resolveIp();
        Thread registrationThread = new Thread(() -> {
            for (int i = 0; i < MAX_RETRY; i++) {
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                    namingService.registerInstance(serviceName, group, registeredIp, port);
                    logger.info("Successfully registered service [{}] to Nacos at {}:{}",
                            serviceName, registeredIp, port);
                    return;
                } catch (NacosException e) {
                    logger.warn("Attempt {}/{} to register service to Nacos failed: {}",
                            i + 1, MAX_RETRY, e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Registration interrupted");
                    return;
                }
            }
            logger.error("Failed to register service [{}] to Nacos after {} attempts", serviceName, MAX_RETRY);
        }, "nacos-registration");
        registrationThread.setDaemon(true);
        registrationThread.start();
    }

    /** IP 解析优先级：配置文件 > 自动检测 > 主机名 > 127.0.0.1 */
    private String resolveIp() {
        if (configuredIp != null && !configuredIp.isBlank()) {
            return configuredIp;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("Failed to resolve local IP, using hostname");
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (Exception ex) {
                return "127.0.0.1";
            }
        }
    }

    @PreDestroy
    public void deregisterService() {
        if (namingService != null && registeredIp != null) {
            try {
                namingService.deregisterInstance(serviceName, group, registeredIp, port);
                logger.info("Successfully deregistered service [{}] from Nacos", serviceName);
            } catch (NacosException e) {
                logger.error("Failed to deregister service from Nacos: {}", e.getMessage(), e);
            }
        }
    }
}
