package com.trademaster.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Operational Readiness Controller for Load Balancers
 * 
 * MANDATORY: Production Operations - TradeMaster Standards
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Zero Trust Security - Rule #6
 * 
 * Provides operational endpoints for infrastructure components
 */
@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
@Slf4j
public class OperationalController {

    private final HealthEndpoint healthEndpoint;
    private final InfoEndpoint infoEndpoint;

    /**
     * Load balancer health check endpoint
     * 
     * MANDATORY: Load Balancer Requirements
     * MANDATORY: Virtual Threads - Rule #12
     */
    @GetMapping("/health")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> loadBalancerHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HealthComponent health = healthEndpoint.health();
                
                Map<String, Object> response = Map.of(
                    "status", health.getStatus().getCode(),
                    "service", "notification-service",
                    "timestamp", LocalDateTime.now().toString(),
                    "version", "2.0.0",
                    "environment", System.getProperty("spring.profiles.active", "unknown")
                );

                return health.getStatus().getCode().equals("UP") 
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(503).body(response);
                    
            } catch (Exception e) {
                log.error("Load balancer health check failed", e);
                return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "service", "notification-service",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
        });
    }

    /**
     * Kubernetes liveness probe
     * 
     * MANDATORY: Container Orchestration
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> livenessProbe() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "probe", "liveness",
            "service", "notification-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Kubernetes readiness probe
     * 
     * MANDATORY: Container Orchestration
     * MANDATORY: Virtual Threads - Rule #12
     */
    @GetMapping("/readiness")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> readinessProbe() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HealthComponent health = healthEndpoint.health();
                boolean ready = health.getStatus().getCode().equals("UP");
                
                Map<String, Object> response = Map.of(
                    "status", ready ? "UP" : "DOWN",
                    "probe", "readiness",
                    "service", "notification-service",
                    "ready", ready,
                    "timestamp", LocalDateTime.now().toString()
                );

                return ready 
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(503).body(response);
                    
            } catch (Exception e) {
                log.error("Readiness probe failed", e);
                return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "probe", "readiness",
                    "service", "notification-service",
                    "ready", false,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
        });
    }

    /**
     * Service startup probe for slow-starting services
     * 
     * MANDATORY: Container Orchestration
     */
    @GetMapping("/startup")
    public ResponseEntity<Map<String, Object>> startupProbe() {
        // Check if service has completed initialization
        boolean started = isServiceFullyStarted();
        
        Map<String, Object> response = Map.of(
            "status", started ? "UP" : "DOWN",
            "probe", "startup",
            "service", "notification-service",
            "started", started,
            "timestamp", LocalDateTime.now().toString()
        );

        return started 
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(503).body(response);
    }

    /**
     * Deep health check for detailed diagnostics
     * 
     * MANDATORY: Production Monitoring
     * MANDATORY: Virtual Threads - Rule #12
     */
    @GetMapping("/health/deep")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deepHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HealthComponent health = healthEndpoint.health();
                Map<String, Object> info = infoEndpoint.info();
                
                Map<String, Object> response = Map.of(
                    "status", health.getStatus().getCode(),
                    "details", health.getDetails(),
                    "info", info,
                    "service", "notification-service",
                    "timestamp", LocalDateTime.now().toString(),
                    "jvm", getJvmInfo(),
                    "system", getSystemInfo()
                );

                return health.getStatus().getCode().equals("UP") 
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(503).body(response);
                    
            } catch (Exception e) {
                log.error("Deep health check failed", e);
                return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "service", "notification-service",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
        });
    }

    /**
     * Service information endpoint
     * 
     * MANDATORY: Operations Visibility
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        try {
            Map<String, Object> info = infoEndpoint.info();
            return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "version", "2.0.0",
                "build", info.getOrDefault("build", Map.of()),
                "git", info.getOrDefault("git", Map.of()),
                "java", getJavaInfo(),
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("Service info failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "service", "notification-service",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    /**
     * Graceful shutdown signal endpoint
     * 
     * MANDATORY: Container Orchestration
     */
    @GetMapping("/shutdown-ready")
    public ResponseEntity<Map<String, Object>> shutdownReady() {
        // Check if service can be safely shut down
        boolean canShutdown = isShutdownSafe();
        
        Map<String, Object> response = Map.of(
            "ready_for_shutdown", canShutdown,
            "service", "notification-service",
            "active_jobs", getActiveJobCount(),
            "pending_notifications", getPendingNotificationCount(),
            "timestamp", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Check if service is fully started
     */
    private boolean isServiceFullyStarted() {
        try {
            // Verify all critical components are initialized
            return healthEndpoint != null && infoEndpoint != null;
        } catch (Exception e) {
            log.debug("Service startup check failed", e);
            return false;
        }
    }

    /**
     * Check if shutdown is safe
     */
    private boolean isShutdownSafe() {
        try {
            // Check for active processing jobs
            int activeJobs = getActiveJobCount();
            int pendingNotifications = getPendingNotificationCount();
            
            return activeJobs == 0 && pendingNotifications < 10;
        } catch (Exception e) {
            log.debug("Shutdown safety check failed", e);
            return false; // Conservative approach - not safe to shutdown if check fails
        }
    }

    /**
     * Get active job count
     */
    private int getActiveJobCount() {
        // In production, integrate with actual job tracking
        return 0;
    }

    /**
     * Get pending notification count
     */
    private int getPendingNotificationCount() {
        // In production, integrate with queue monitoring
        return 0;
    }

    /**
     * Get JVM information
     */
    private Map<String, Object> getJvmInfo() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
            "max_memory", runtime.maxMemory(),
            "total_memory", runtime.totalMemory(),
            "free_memory", runtime.freeMemory(),
            "used_memory", runtime.totalMemory() - runtime.freeMemory(),
            "processors", runtime.availableProcessors(),
            "java_version", System.getProperty("java.version"),
            "virtual_threads", isVirtualThreadsEnabled()
        );
    }

    /**
     * Get system information
     */
    private Map<String, Object> getSystemInfo() {
        return Map.of(
            "os_name", System.getProperty("os.name"),
            "os_version", System.getProperty("os.version"),
            "os_arch", System.getProperty("os.arch"),
            "user_timezone", System.getProperty("user.timezone"),
            "file_encoding", System.getProperty("file.encoding")
        );
    }

    /**
     * Get Java information
     */
    private Map<String, Object> getJavaInfo() {
        return Map.of(
            "version", System.getProperty("java.version"),
            "vendor", System.getProperty("java.vendor"),
            "runtime", System.getProperty("java.runtime.name"),
            "vm_name", System.getProperty("java.vm.name"),
            "vm_version", System.getProperty("java.vm.version"),
            "virtual_threads_enabled", isVirtualThreadsEnabled()
        );
    }

    /**
     * Check if virtual threads are enabled
     */
    private boolean isVirtualThreadsEnabled() {
        try {
            return System.getProperty("spring.threads.virtual.enabled", "false").equals("true");
        } catch (Exception e) {
            return false;
        }
    }
}