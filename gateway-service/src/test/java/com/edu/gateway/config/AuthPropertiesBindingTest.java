package com.edu.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @Import(AuthProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("AuthProperties - default whitelist is non-null and empty")
    void authProperties_defaultWhitelist() {
        contextRunner.run(context -> {
            AuthProperties props = context.getBean(AuthProperties.class);
            assertThat(props.getWhitelist()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("AuthProperties - binds app.auth.whitelist from properties")
    void authProperties_bindsWhitelist() {
        contextRunner
                .withPropertyValues(
                        "app.auth.whitelist[0]=/api/auth/**",
                        "app.auth.whitelist[1]=/api/public/**",
                        "app.auth.whitelist[2]=/actuator/health"
                )
                .run(context -> {
                    AuthProperties props = context.getBean(AuthProperties.class);
                    assertThat(props.getWhitelist()).containsExactly(
                            "/api/auth/**",
                            "/api/public/**",
                            "/actuator/health"
                    );
                });
    }
}

