package com.edu.gateway.config;

import com.alibaba.nacos.api.naming.NamingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.main.web-application-type=reactive",
                "nacos.discovery.server-addr=localhost:8848",
                "nacos.discovery.namespace=public"
        }
)
@ActiveProfiles("test")
@DisplayName("Gateway RouteLocator Wiring Tests")
class GatewayRoutesIntegrationTest {

    // Prevent real Nacos initialization in tests.
    @MockitoBean(name = "namingService")
    private NamingService namingService;

    @Autowired
    private RouteLocator routeLocator;

    private static GatewayFilter unwrap(GatewayFilter filter) {
        GatewayFilter current = filter;
        while (current instanceof OrderedGatewayFilter ordered) {
            current = ordered.getDelegate();
        }
        return current;
    }

    @Test
    @DisplayName("RouteLocator - registers expected routes with rate limiting + circuit breaker filters")
    void routeLocator_registersExpectedRoutes() {
        List<Route> routes = routeLocator.getRoutes()
                .collectList()
                .block(Duration.ofSeconds(3));

        assertThat(routes).isNotNull();

        Map<String, Route> byId = routes.stream().collect(Collectors.toMap(Route::getId, r -> r, (a, b) -> a));
        assertThat(byId.keySet()).containsExactlyInAnyOrder(
                "user-service-auth",
                "user-service-users",
                "course-service",
                "enrollment-service",
                "progress-service",
                "search-service"
        );

        // Ensure ids are unique (no silent overwrite / duplicates).
        assertThat(byId.keySet()).hasSize(routes.size());

        assertThat(byId.get("user-service-auth").getUri().toString()).isEqualTo("lb://user-service");
        assertThat(byId.get("user-service-users").getUri().toString()).isEqualTo("lb://user-service");
        assertThat(byId.get("course-service").getUri().toString()).isEqualTo("lb://course-service");
        assertThat(byId.get("enrollment-service").getUri().toString()).isEqualTo("lb://course-service");
        assertThat(byId.get("progress-service").getUri().toString()).isEqualTo("lb://course-service");
        assertThat(byId.get("search-service").getUri().toString()).isEqualTo("lb://search-service");

        for (String id : byId.keySet()) {
            Route route = byId.get(id);

            // The Java DSL attaches both filters; assert by class name to avoid brittle equality checks.
            Set<String> filterClassNames = route.getFilters().stream()
                    .map(GatewayRoutesIntegrationTest::unwrap)
                    .map(f -> f.getClass().getName())
                    .collect(Collectors.toSet());

            assertThat(filterClassNames)
                    .anyMatch(name -> name.contains("RequestRateLimiter"));
            assertThat(filterClassNames)
                    .anyMatch(name -> name.contains("CircuitBreaker"));
        }
    }
}
