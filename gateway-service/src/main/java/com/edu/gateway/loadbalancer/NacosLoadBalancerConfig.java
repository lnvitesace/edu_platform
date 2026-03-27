package com.edu.gateway.loadbalancer;

import com.alibaba.nacos.api.naming.NamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 用自定义的 Nacos 实例列表替换 Spring Cloud LoadBalancer 默认的服务发现。
 * <p>
 * @LoadBalancerClients(defaultConfiguration) 使所有 lb:// 路由都使用 Nacos 解析，
 * 无需为每个服务单独配置。
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = NacosLoadBalancerConfig.class)
@RequiredArgsConstructor
public class NacosLoadBalancerConfig {

    private final NamingService namingService;

    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new NacosServiceInstanceListSupplier(serviceId, namingService);
    }
}
