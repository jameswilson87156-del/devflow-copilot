package com.devflow.copilot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SharedProviderConfigurationMappingTest {

    @Test
    void projectSpecificSettingsTakePriorityOverSharedSettings() {
        runWith(Map.of(
                "DEVFLOW_AI_PROVIDER", "project-specific",
                "PORTFOLIO_AI_PROVIDER", "shared"
        )).run(context -> assertThat(context.getBean(AiProviderProperties.class).getProvider())
                .isEqualTo("project-specific"));
    }

    @Test
    void sharedSettingsAreUsedWhenProjectSpecificSettingsAreAbsent() {
        runWith(Map.of(
                "PORTFOLIO_AI_PROTOCOL", "chat-completions-compatible"
        )).run(context -> assertThat(context.getBean(AiProviderProperties.class).getProtocol())
                .isEqualTo("chat-completions-compatible"));
    }

    @Test
    void projectSpecificProtocolOverridesTheSharedCapabilityMarker() {
        runWith(Map.of(
                "DEVFLOW_AI_PROTOCOL", "project-protocol",
                "PORTFOLIO_AI_PROTOCOL", "shared-protocol"
        )).run(context -> assertThat(context.getBean(AiProviderProperties.class).getProtocol())
                .isEqualTo("project-protocol"));
    }

    @Test
    void safeDefaultsApplyWhenNeitherProjectNorSharedSettingsExist() {
        runWith(Map.of()).run(context -> {
            AiProviderProperties properties = context.getBean(AiProviderProperties.class);
            assertThat(properties.getProvider()).isEqualTo("local-rule");
            assertThat(properties.isFallbackToLocal()).isTrue();
        });
    }

    @Test
    void projectSpecificFallbackSettingTakesPriorityOverSharedSetting() {
        runWith(Map.of(
                "DEVFLOW_AI_FALLBACK_TO_LOCAL", "false",
                "PORTFOLIO_AI_FALLBACK_ENABLED", "true"
        )).run(context -> assertThat(context.getBean(AiProviderProperties.class).isFallbackToLocal()).isFalse());
    }

    private ApplicationContextRunner runWith(Map<String, Object> values) {
        return new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-variables", values));
                    context.getEnvironment().getPropertySources().addLast(new MapPropertySource("application-yaml", applicationYaml()));
                });
    }

    private Map<String, Object> applicationYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        Map<String, Object> result = new LinkedHashMap<>();
        properties.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProviderProperties.class)
    static class PropertiesConfiguration {
    }
}
