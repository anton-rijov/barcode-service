package com.x5.food.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/health")
public class HealthController {

    private static final double MEGABYTES = 1024.0 * 1024.0;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HealthController(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        var response = new HashMap<String, Object>();

        // Получаем данные из actuator health
        JsonNode healthData = getActuatorData("/actuator/health");

        // Получаем данные из actuator info
        JsonNode infoData = getActuatorData("/actuator/info");

        // Базовая системная информация
        response.put("os", Map.of(
                "name", System.getProperty("os.name"),
                "version", System.getProperty("os.version"),
                "arch", System.getProperty("os.arch")
        ));

        response.put("jvm", Map.of(
                "processors", Runtime.getRuntime().availableProcessors(),
                "total_memory_mb", String.format("%.2f", Runtime.getRuntime().totalMemory() / MEGABYTES),
                "max_memory_mb", String.format("%.2f", Runtime.getRuntime().maxMemory() / MEGABYTES)
        ));

        // Данные приложения из info endpoint
        if (infoData != null && infoData.has("app")) {
            response.put("app", infoData.get("app"));
        }

        // Статус БД из health endpoint
        if (healthData != null) {
            response.put("database", extractDbStatus(healthData));
        }

        // Общий статус приложения
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    private JsonNode getActuatorData(String endpoint) {
        try {
            String response = restTemplate.getForObject("http://localhost:8080" + endpoint, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> extractDbStatus(JsonNode healthData) {
        var dbStatus = new HashMap<String, Object>();

        try {
            var overallStatus = healthData.path("status").asText("UNKNOWN");
            dbStatus.put("overall_status", overallStatus);

            JsonNode components = healthData.path("components");
            if (!components.isMissingNode()) {
                JsonNode db = components.path("db");
                if (!db.isMissingNode()) {
                    var status = db.path("status").asText("UNKNOWN");
                    dbStatus.put("db_status", status);
                    dbStatus.put("connected", "UP".equals(status));
                }
            }
        } catch (Exception e) {
            dbStatus.put("error", "Cannot extract DB status");
        }

        return dbStatus;
    }

    @GetMapping("/echo")
    public String handshake(@RequestParam(required = false) String message) {
        return message != null ? message : "Hello!";
    }
}