package com.eduplatform.eduplatform_backend.monitoring.web;

import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.monitoring.web.dto.SystemHealthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/super/system")
@Tag(name = "Super — System Monitoring")
public class SystemMonitoringController {

    private final ObjectProvider<HealthEndpoint> healthEndpoint;

    public SystemMonitoringController(ObjectProvider<HealthEndpoint> healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('system:read')")
    @Operation(summary = "Overall system health, component statuses and JVM runtime stats")
    public ApiResponse<SystemHealthDto> health() {
        String status = "UNKNOWN";
        Map<String, String> components = new LinkedHashMap<>();

        HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
        if (endpoint != null) {
            try {
                HealthComponent health = endpoint.health();
                if (health != null) {
                    status = codeOf(health);
                    if (health instanceof CompositeHealth composite) {
                        Map<String, HealthComponent> children = composite.getComponents();
                        if (children != null) {
                            children.forEach((name, component) ->
                                    components.put(name, codeOf(component)));
                        }
                    }
                }
            } catch (RuntimeException ex) {
                // Never let a health probe failure break the endpoint.
                status = "UNKNOWN";
                components.clear();
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        int availableProcessors = runtime.availableProcessors();
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        String javaVersion = System.getProperty("java.version");

        SystemHealthDto dto = new SystemHealthDto(
                status,
                components,
                usedMemory,
                maxMemory,
                totalMemory,
                availableProcessors,
                uptimeMillis,
                javaVersion);

        return ApiResponse.ok(dto);
    }

    private static String codeOf(HealthComponent component) {
        if (component == null || component.getStatus() == null) {
            return "UNKNOWN";
        }
        return component.getStatus().getCode();
    }
}
