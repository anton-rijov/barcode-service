package com.x5.food.controller;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/health")
public class HealthController {

    private final RestTemplate restTemplate;

    public HealthController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> response = new HashMap<>();

        // Получаем данные из actuator health
        Map<String, Object> healthData = getActuatorData("/actuator/health");

        // Базовая системная информация
        response.put("os", Map.of("name", System.getProperty("os.name"),
                "version", System.getProperty("os.version"),
                "arch", System.getProperty("os.arch")));

        response.put("jvm", Map.of("processors", Runtime.getRuntime().availableProcessors(),
                "total_memory_mb", String.format("%.2f", Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0)),
                "max_memory_mb", String.format("%.2f", Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0))));

        // Получаем данные из actuator info
        Map<String, Object> infoData = getActuatorData("/actuator/info");

        response.put("app", infoData.get("app"));

        // Статус БД из health endpoint
        if (healthData != null) {
            response.put("database", extractDbStatus(healthData));
        }

        // Общий статус приложения
        response.put("status", "OK");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> getActuatorData(String endpoint) {
        try {
            return restTemplate.getForObject("http://localhost:8080" + endpoint, Map.class);
        } catch (Exception e) {
            return Map.of("error", "Endpoint unavailable: " + endpoint);
        }
    }

    private Map<String, Object> extractDbStatus(Map<String, Object> healthData) {
        Map<String, Object> dbStatus = new HashMap<>();

        try {
            String overallStatus = (String) healthData.get("status");
            dbStatus.put("overall_status", overallStatus);

            if (healthData.containsKey("components")) {
                Map<String, Object> components = (Map<String, Object>) healthData.get("components");
                if (components.containsKey("db")) {
                    Map<String, Object> db = (Map<String, Object>) components.get("db");
                    dbStatus.put("db_status", db.get("status"));
                    dbStatus.put("connected", "UP".equals(db.get("status")));
                }
            }
        } catch (Exception e) {
            dbStatus.put("error", "Cannot extract DB status");
        }

        return dbStatus;
    }

    @GetMapping("/echo")
    public String handshake(@RequestParam(required = false) String message) {
        return message;
    }
}