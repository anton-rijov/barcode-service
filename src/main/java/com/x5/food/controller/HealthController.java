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
    public ResponseEntity<JsonNode> getSystemStatus() {
        var response = objectMapper.createObjectNode();

        response.with("os")
                .put("name", System.getProperty("os.name"))
                .put("version", System.getProperty("os.version"))
                .put("arch", System.getProperty("os.arch"));

        response.with("jvm")
                .put("processors", Runtime.getRuntime().availableProcessors())
                .put("total_memory_mb", String.format("%.2f", Runtime.getRuntime().totalMemory() / MEGABYTES))
                .put("max_memory_mb", String.format("%.2f", Runtime.getRuntime().maxMemory() / MEGABYTES));

        if (getActuatorData("/actuator/info") instanceof JsonNode infoData
                && infoData.has("app")) {
            response.put("app", infoData.get("app"));
        }

        if (getActuatorData("/actuator/health") instanceof JsonNode healthData) {
            response.set("database", extractDbStatus(healthData));
        }

        response.put("status", "OK");

        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    private JsonNode getActuatorData(String endpoint) {
        try {
            String url = "http://localhost:8080" + endpoint;
            return objectMapper.readTree(restTemplate.getForObject(url, String.class));
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode extractDbStatus(JsonNode healthData) {
        var dbStatus = objectMapper.createObjectNode();

        try {
            var overallStatus = healthData.path("status").asText("UNKNOWN");
            dbStatus.put("overall_status", overallStatus);

            var components = healthData.path("components");
            if (!components.isMissingNode()) {
                var db = components.path("db");
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