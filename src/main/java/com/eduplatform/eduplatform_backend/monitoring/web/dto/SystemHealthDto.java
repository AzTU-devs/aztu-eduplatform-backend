package com.eduplatform.eduplatform_backend.monitoring.web.dto;

import java.util.Map;

/**
 * Snapshot of system/runtime health for the super-admin console.
 * {@code status} is the overall actuator status (UP/DOWN/UNKNOWN), and
 * {@code components} maps each health contributor name to its status code.
 */
public record SystemHealthDto(
        String status,
        Map<String, String> components,
        long jvmUsedBytes,
        long jvmMaxBytes,
        long jvmTotalBytes,
        int availableProcessors,
        long uptimeMillis,
        String javaVersion
) {}
