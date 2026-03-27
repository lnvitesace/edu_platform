package com.edu.gateway.loadbalancer;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("NacosLoadBalancerConfig Unit Tests")
class NacosLoadBalancerConfigTest {

    @Test
    @DisplayName("serviceInstanceListSupplier - uses LoadBalancerClientFactory.PROPERTY_NAME as serviceId")
    void serviceInstanceListSupplier_usesServiceIdFromEnvironment() throws Exception {
        NamingService namingService = mock(NamingService.class);
        Instance instance = new Instance();
        instance.setInstanceId("inst-1");
        instance.setIp("127.0.0.1");
        instance.setPort(8001);
        when(namingService.selectInstances(eq("user-service"), eq(true))).thenReturn(List.of(instance));

        NacosLoadBalancerConfig config = new NacosLoadBalancerConfig(namingService);

        Environment env = new MockEnvironment()
                .withProperty(LoadBalancerClientFactory.PROPERTY_NAME, "user-service");

        var supplier = config.serviceInstanceListSupplier(env, null);
        assertThat(supplier.getServiceId()).isEqualTo("user-service");

        StepVerifier.create(supplier.get())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ServiceInstance si = list.get(0);
                    assertThat(si.getServiceId()).isEqualTo("user-service");
                })
                .verifyComplete();
    }
}

