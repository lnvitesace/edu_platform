package com.edu.gateway.loadbalancer;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * 从 Nacos NamingService 获取健康实例列表，适配 Spring Cloud LoadBalancer SPI。
 * <p>
 * 每次调用 get() 都实时查询 Nacos（selectInstances 内部有本地缓存），
 * 确保路由能及时感知实例上下线。查询失败时返回空列表而非抛异常，
 * 让熔断器有机会触发 fallback。
 */
@Slf4j
public class NacosServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final NamingService namingService;

    public NacosServiceInstanceListSupplier(String serviceId, NamingService namingService) {
        this.serviceId = serviceId;
        this.namingService = namingService;
    }

    @Override
    public @NonNull String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return Flux.defer(() -> {
            try {
                List<Instance> instances = namingService.selectInstances(serviceId, true);
                if (instances.isEmpty()) {
                    log.warn("No healthy instances found for service: {}", serviceId);
                    return Flux.just(Collections.emptyList());
                }

                List<ServiceInstance> serviceInstances = instances.stream()
                        .map(this::toServiceInstance)
                        .toList();

                log.debug("Found {} instances for service: {}", serviceInstances.size(), serviceId);
                return Flux.just(serviceInstances);
            } catch (Exception e) {
                log.error("Failed to get instances for service: {}", serviceId, e);
                return Flux.just(Collections.emptyList());
            }
        });
    }

    private ServiceInstance toServiceInstance(Instance instance) {
        return new DefaultServiceInstance(
                instance.getInstanceId(),
                serviceId,
                instance.getIp(),
                instance.getPort(),
                false,
                instance.getMetadata()
        );
    }
}
