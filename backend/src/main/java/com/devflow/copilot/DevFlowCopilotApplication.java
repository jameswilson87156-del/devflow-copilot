package com.devflow.copilot;

import com.devflow.copilot.config.AiProviderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.devflow.copilot.mapper")
@SpringBootApplication
@EnableConfigurationProperties(AiProviderProperties.class)
public class DevFlowCopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevFlowCopilotApplication.class, args);
    }
}
