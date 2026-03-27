package com.edu.gateway.loadbalancer;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("NacosServiceInstanceListSupplier Unit Tests")
class NacosServiceInstanceListSupplierTest {

    @Test
    @DisplayName("get - returns empty list when no healthy instances")
    void get_noInstances_returnsEmptyList() throws Exception {
        NamingService namingService = mock(NamingService.class);
        when(namingService.selectInstances(eq("user-service"), eq(true))).thenReturn(List.of());

        NacosServiceInstanceListSupplier supplier = new NacosServiceInstanceListSupplier("user-service", namingService);

        StepVerifier.create(supplier.get())
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("get - maps Nacos Instance to Spring ServiceInstance")
    void get_withInstances_mapsCorrectly() throws Exception {
        NamingService namingService = mock(NamingService.class);
        Instance instance = new Instance();
        instance.setInstanceId("inst-1");
        instance.setIp("10.0.0.10");
        instance.setPort(8001);
        instance.setMetadata(Map.of("zone", "z1"));

        when(namingService.selectInstances(eq("user-service"), eq(true))).thenReturn(List.of(instance));

        NacosServiceInstanceListSupplier supplier = new NacosServiceInstanceListSupplier("user-service", namingService);

        StepVerifier.create(supplier.get())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ServiceInstance si = list.get(0);
                    assertThat(si.getServiceId()).isEqualTo("user-service");
                    assertThat(si.getHost()).isEqualTo("10.0.0.10");
                    assertThat(si.getPort()).isEqualTo(8001);
                    assertThat(si.isSecure()).isFalse();
                    assertThat(si.getMetadata()).containsEntry("zone", "z1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("get - returns empty list when NamingService throws")
    void get_whenNamingServiceThrows_returnsEmptyList() throws Exception {
        NamingService namingService = mock(NamingService.class);
        when(namingService.selectInstances(eq("user-service"), eq(true)))
                .thenThrow(new RuntimeException("boom"));

        NacosServiceInstanceListSupplier supplier = new NacosServiceInstanceListSupplier("user-service", namingService);

        StepVerifier.create(supplier.get())
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }
}

