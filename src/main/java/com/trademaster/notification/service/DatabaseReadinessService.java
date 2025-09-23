package com.trademaster.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Database Readiness Service for Production Monitoring
 * 
 * MANDATORY: Database Readiness - TradeMaster Standards
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * 
 * Monitors database health, migration status, and performance metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseReadinessService implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private static final String HEALTH_CHECK_QUERY = "SELECT 1";
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 10;

    /**
     * Comprehensive database health check
     * 
     * MANDATORY: Production Monitoring
     * MANDATORY: Virtual Threads - Rule #12
     */
    @Override
    public Health health() {
        return CompletableFuture
            .supplyAsync(this::performHealthCheck)
            .orTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                log.error("Database health check failed", throwable);
                return Health.down()
                    .withDetail("error", throwable.getMessage())
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .withDetail("component", "database")
                    .build();
            })
            .join();
    }

    /**
     * Perform comprehensive database health check
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private Health performHealthCheck() {
        try {
            // Basic connectivity test
            boolean connected = checkDatabaseConnectivity();
            if (!connected) {
                return Health.down()
                    .withDetail("connectivity", "FAILED")
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .build();
            }

            // Migration status check
            Map<String, Object> migrationStatus = checkMigrationStatus();
            
            // Performance metrics
            Map<String, Object> performanceMetrics = getPerformanceMetrics();
            
            // Schema validation
            Map<String, Object> schemaValidation = validateDatabaseSchema();
            
            // Connection pool health
            Map<String, Object> connectionPoolHealth = getConnectionPoolHealth();

            boolean allHealthy = (Boolean) migrationStatus.get("migrations_current") &&
                               (Boolean) schemaValidation.get("schema_valid") &&
                               (Boolean) connectionPoolHealth.get("pool_healthy");

            return allHealthy 
                ? Health.up()
                    .withDetail("connectivity", "UP")
                    .withDetail("migrations", migrationStatus)
                    .withDetail("performance", performanceMetrics)
                    .withDetail("schema", schemaValidation)
                    .withDetail("connection_pool", connectionPoolHealth)
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .build()
                : Health.down()
                    .withDetail("connectivity", "UP")
                    .withDetail("migrations", migrationStatus)
                    .withDetail("performance", performanceMetrics)
                    .withDetail("schema", schemaValidation)
                    .withDetail("connection_pool", connectionPoolHealth)
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .withDetail("timestamp", LocalDateTime.now().toString())
                .build();
        }
    }

    /**
     * Check basic database connectivity
     */
    private boolean checkDatabaseConnectivity() {
        try {
            Integer result = jdbcTemplate.queryForObject(HEALTH_CHECK_QUERY, Integer.class);
            return result != null && result.equals(1);
        } catch (Exception e) {
            log.warn("Database connectivity check failed", e);
            return false;
        }
    }

    /**
     * Check migration status (Flyway-based with fallback)
     *
     * MANDATORY: Functional Programming - Rule #3
     */
    private Map<String, Object> checkMigrationStatus() {
        try {
            // Check if Flyway schema history table exists
            boolean schemaHistoryExists = checkTableExists("flyway_schema_history");

            if (schemaHistoryExists) {
                // Flyway table exists - check its status
                Integer migrationCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history",
                    Integer.class
                );

                String lastMigration = jdbcTemplate.queryForObject(
                    "SELECT version FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 1",
                    String.class
                );

                Boolean hasFailedMigrations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) > 0 FROM flyway_schema_history WHERE success = false",
                    Boolean.class
                );

                return Map.of(
                    "migrations_current", !hasFailedMigrations && migrationCount > 0,
                    "migration_count", migrationCount != null ? migrationCount : 0,
                    "last_migration", lastMigration != null ? lastMigration : "none",
                    "has_failures", hasFailedMigrations != null ? hasFailedMigrations : false,
                    "migration_tool", "flyway",
                    "schema_history_exists", true
                );
            } else {
                // Flyway table doesn't exist - check if required tables exist as alternative
                String[] criticalTables = {
                    "notification_audit_log",
                    "notification_metrics",
                    "notification_errors"
                };

                boolean allCriticalTablesExist = true;
                for (String table : criticalTables) {
                    if (!checkTableExists(table)) {
                        allCriticalTablesExist = false;
                        break;
                    }
                }

                return Map.of(
                    "migrations_current", allCriticalTablesExist,
                    "migration_count", 0,
                    "last_migration", "tables-exist-check",
                    "has_failures", false,
                    "migration_tool", "flyway",
                    "schema_history_exists", false,
                    "fallback_check", "critical-tables-exist",
                    "critical_tables_exist", allCriticalTablesExist
                );
            }

        } catch (Exception e) {
            log.warn("Migration status check failed", e);
            return Map.of(
                "migrations_current", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName(),
                "migration_tool", "flyway"
            );
        }
    }

    /**
     * Get database performance metrics
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private Map<String, Object> getPerformanceMetrics() {
        try {
            // Query execution time test
            long startTime = System.currentTimeMillis();
            jdbcTemplate.execute("SELECT COUNT(*) FROM information_schema.tables");
            long queryTime = System.currentTimeMillis() - startTime;

            // Database size
            Long dbSize = jdbcTemplate.queryForObject(
                "SELECT pg_database_size(current_database())", 
                Long.class
            );

            // Active connections
            Integer activeConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()", 
                Integer.class
            );

            return Map.of(
                "query_response_time_ms", queryTime,
                "database_size_bytes", dbSize != null ? dbSize : 0L,
                "database_size_mb", dbSize != null ? dbSize / 1024.0 / 1024.0 : 0.0,
                "active_connections", activeConnections != null ? activeConnections : 0,
                "performance_status", queryTime < 1000 ? "GOOD" : queryTime < 5000 ? "WARNING" : "POOR"
            );

        } catch (Exception e) {
            log.warn("Performance metrics collection failed", e);
            return Map.of(
                "performance_status", "ERROR",
                "error", e.getMessage()
            );
        }
    }

    /**
     * Validate critical database schema elements
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private Map<String, Object> validateDatabaseSchema() {
        try {
            // Check for critical tables
            String[] criticalTables = {
                "notification_audit_log", 
                "notification_metrics", 
                "notification_errors"
            };
            
            boolean allTablesExist = true;
            StringBuilder missingTables = new StringBuilder();
            
            for (String table : criticalTables) {
                if (!checkTableExists(table)) {
                    allTablesExist = false;
                    if (missingTables.length() > 0) missingTables.append(", ");
                    missingTables.append(table);
                }
            }

            // Check for critical views
            boolean healthViewExists = checkViewExists("v_notification_health_summary");
            boolean performanceViewExists = checkViewExists("v_notification_performance_metrics");

            return Map.of(
                "schema_valid", allTablesExist && healthViewExists && performanceViewExists,
                "critical_tables_exist", allTablesExist,
                "missing_tables", missingTables.toString(),
                "health_view_exists", healthViewExists,
                "performance_view_exists", performanceViewExists,
                "schema_version", "2.0"
            );

        } catch (Exception e) {
            log.warn("Schema validation failed", e);
            return Map.of(
                "schema_valid", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Check connection pool health
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private Map<String, Object> getConnectionPoolHealth() {
        try {
            // This would be implementation-specific based on connection pool (HikariCP, etc.)
            // For now, provide basic metrics
            Integer totalConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity", 
                Integer.class
            );
            
            Integer activeConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'", 
                Integer.class
            );

            boolean poolHealthy = totalConnections != null && totalConnections < 100 && 
                                activeConnections != null && activeConnections < 50;

            return Map.of(
                "pool_healthy", poolHealthy,
                "total_connections", totalConnections != null ? totalConnections : 0,
                "active_connections", activeConnections != null ? activeConnections : 0,
                "idle_connections", totalConnections != null && activeConnections != null ? 
                    totalConnections - activeConnections : 0,
                "pool_status", poolHealthy ? "HEALTHY" : "WARNING"
            );

        } catch (Exception e) {
            log.warn("Connection pool health check failed", e);
            return Map.of(
                "pool_healthy", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Check if table exists
     */
    private boolean checkTableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = 'public'", 
                Integer.class, 
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Table existence check failed for {}", tableName, e);
            return false;
        }
    }

    /**
     * Check if view exists
     */
    private boolean checkViewExists(String viewName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.views WHERE table_name = ? AND table_schema = 'public'", 
                Integer.class, 
                viewName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("View existence check failed for {}", viewName, e);
            return false;
        }
    }

    /**
     * Execute database health diagnostic
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Map<String, Object>> performDatabaseDiagnostic() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use the built-in PostgreSQL health check function
                Map<String, Object> healthData = jdbcTemplate.queryForObject(
                    "SELECT check_database_health() as health_check", 
                    (rs, rowNum) -> {
                        String jsonData = rs.getString("health_check");
                        // In production, parse this JSON properly
                        return Map.of("health_data", jsonData);
                    }
                );
                
                return Map.of(
                    "diagnostic_timestamp", LocalDateTime.now().toString(),
                    "service", "notification-service",
                    "database_health", healthData,
                    "status", "completed"
                );
                
            } catch (Exception e) {
                log.error("Database diagnostic failed", e);
                return Map.of(
                    "diagnostic_timestamp", LocalDateTime.now().toString(),
                    "service", "notification-service",
                    "status", "failed",
                    "error", e.getMessage()
                );
            }
        });
    }
}