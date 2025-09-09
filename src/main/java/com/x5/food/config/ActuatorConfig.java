package com.x5.food.config;

import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ActuatorConfig {

    @Bean
    public InfoContributor simpleInfoContributor() {
        return builder -> {
            builder.withDetail("app", Map.of(
                    "name", "barcode-service",
                    "version", "1.0.0",
                    "description", "Barcode Lookup Service"
            ));
        };
    }
}