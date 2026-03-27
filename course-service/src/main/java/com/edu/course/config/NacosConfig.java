package com.edu.course.config;

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
 *
 * 不使用 Spring Cloud Nacos 自动注册，而是手动管理注册生命周期，原因：
 * 1. 需要在应用完全就绪后才注册（ApplicationReadyEvent），避免流量打到未初始化的实例
 * 2. 支持带重试的注册策略，应对 Nacos 服务器启动慢于应用的场景
 * 3. 应用关闭时主动注销，确保服务列表及时更新
 *
 * 可通过 nacos.discovery.enabled=false 禁用（如本地开发/测试场景）。
 */
@Configuration
@ConditionalOnProperty(name = "nacos.discovery.enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfig {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfig.class);

    /** 注册重试次数，应对 Nacos 启动延迟 */
    private static final int MAX_RETRY = 30;
    /** 重试间隔（毫秒） */
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

    /**
     * 应用完全就绪后再注册到 Nacos。
     * 延迟注册可确保所有 Bean 初始化完成，避免接收到请求时服务还未准备好。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerToNacos() {
        this.registeredIp = resolveIp();
        Thread registrationThread = new Thread(() -> {
            for (int i = 0; i < MAX_RETRY; i++) {
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                    namingService.registerInstance(serviceName, group, registeredIp, port);
                    logger.info("Successfully registered service [{}] to Nacos at {}:{}", serviceName, registeredIp, port);
                    return;
                } catch (NacosException e) {
                    logger.warn("Attempt {}/{} to register service to Nacos failed: {}", i + 1, MAX_RETRY, e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Registration interrupted");
                    return;
                }
            }
            logger.error("Failed to register service to Nacos after {} attempts", MAX_RETRY);
        }, "nacos-registration");
        registrationThread.setDaemon(true);
        registrationThread.start();
    }

    /**
     * 解析服务实例 IP。
     * 优先使用配置指定的 IP（容器/K8s 场景），否则自动探测本机地址。
     */
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

    /**
     * 应用关闭时主动注销服务实例。
     * 比依赖心跳超时更快地从服务列表移除，减少调用方请求到已关闭实例的概率。
     */
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
