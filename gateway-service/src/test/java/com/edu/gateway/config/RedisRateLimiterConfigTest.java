package com.edu.gateway.config;

import com.alibaba.nacos.api.naming.NamingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.main.web-application-type=reactive",
                "nacos.discovery.server-addr=localhost:8848",
                "nacos.discovery.namespace=public",
                "app.rate-limit.replenish-rate=5",
                "app.rate-limit.burst-capacity=7",
                "app.rate-limit.requested-tokens=2"
        }
)
@ActiveProfiles("test")
@DisplayName("RedisRateLimiter Bean Configuration Tests")
class RedisRateLimiterConfigTest {

    // Prevent real Nacos initialization in tests.
    @MockitoBean(name = "namingService")
    private NamingService namingService;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Test
    @DisplayName("RedisRateLimiter - default config reflects app.rate-limit properties")
    void redisRateLimiter_defaultConfig_matchesProperties() throws Exception {
        Method m = RedisRateLimiter.class.getDeclaredMethod("getDefaultConfig");
        m.setAccessible(true);
        RedisRateLimiter.Config cfg = (RedisRateLimiter.Config) m.invoke(redisRateLimiter);

        assertThat(cfg.getReplenishRate()).isEqualTo(5);
        assertThat(cfg.getBurstCapacity()).isEqualTo(7);
        assertThat(cfg.getRequestedTokens()).isEqualTo(2);
    }
}
