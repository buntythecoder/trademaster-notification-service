package com.trademaster.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * SSL/TLS Configuration and Health Monitoring Service
 * 
 * MANDATORY: SSL/TLS Certificate Management - TradeMaster Standards
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * 
 * Monitors SSL certificate health and expiration
 */
@Service
@Slf4j
public class SSLConfigurationService implements HealthIndicator {

    private static final int SSL_CHECK_TIMEOUT_SECONDS = 10;
    private static final int CERTIFICATE_WARNING_DAYS = 30;

    /**
     * SSL/TLS health check for load balancer readiness
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    @Override
    public Health health() {
        return CompletableFuture
            .supplyAsync(this::performSSLHealthCheck)
            .orTimeout(SSL_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                log.error("SSL health check failed", throwable);
                return Health.down()
                    .withDetail("ssl_status", "FAILED")
                    .withDetail("error", throwable.getMessage())
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .build();
            })
            .join();
    }

    /**
     * Perform comprehensive SSL health check
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch expressions
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private Health performSSLHealthCheck() {
        try {
            boolean sslEnabled = isSSLEnabled();

            // Rule #3: NO if-else, use Optional.filter().map().orElseGet()
            return java.util.Optional.of(sslEnabled)
                .filter(Boolean::booleanValue)
                .map(_ -> buildSSLEnabledHealthCheck())
                .orElseGet(() -> Health.up()
                    .withDetail("ssl_status", "DISABLED")
                    .withDetail("ssl_enabled", false)
                    .withDetail("timestamp", LocalDateTime.now().toString())
                    .build());

        } catch (Exception e) {
            log.error("SSL health check failed", e);
            return Health.down()
                .withDetail("ssl_status", "ERROR")
                .withDetail("error", e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .withDetail("timestamp", LocalDateTime.now().toString())
                .build();
        }
    }

    /**
     * Build SSL health check when SSL is enabled
     *
     * MANDATORY: Rule #14 - Pattern Matching with switch expression
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    private Health buildSSLEnabledHealthCheck() {
        Map<String, Object> keystoreHealth = checkKeystoreHealth();
        Map<String, Object> truststoreHealth = checkTruststoreHealth();
        Map<String, Object> certificateHealth = checkCertificateExpiration();
        Map<String, Object> cipherSuiteHealth = checkCipherSuiteConfiguration();

        boolean allHealthy = (Boolean) keystoreHealth.getOrDefault("keystore_valid", false) &&
                           (Boolean) truststoreHealth.getOrDefault("truststore_valid", false) &&
                           (Boolean) certificateHealth.getOrDefault("certificate_valid", false) &&
                           (Boolean) cipherSuiteHealth.getOrDefault("cipher_suites_secure", false);

        // Rule #14: Use switch expression instead of ternary
        return switch (allHealthy) {
            case true -> Health.up()
                .withDetail("ssl_status", "HEALTHY")
                .withDetail("ssl_enabled", true)
                .withDetail("keystore", keystoreHealth)
                .withDetail("truststore", truststoreHealth)
                .withDetail("certificates", certificateHealth)
                .withDetail("cipher_suites", cipherSuiteHealth)
                .withDetail("timestamp", LocalDateTime.now().toString())
                .build();
            case false -> Health.down()
                .withDetail("ssl_status", "UNHEALTHY")
                .withDetail("ssl_enabled", true)
                .withDetail("keystore", keystoreHealth)
                .withDetail("truststore", truststoreHealth)
                .withDetail("certificates", certificateHealth)
                .withDetail("cipher_suites", cipherSuiteHealth)
                .withDetail("timestamp", LocalDateTime.now().toString())
                .build();
        };
    }

    /**
     * Check if SSL is enabled
     */
    private boolean isSSLEnabled() {
        String sslEnabled = System.getProperty("server.ssl.enabled", 
                           System.getenv("SSL_ENABLED"));
        return "true".equalsIgnoreCase(sslEnabled);
    }

    /**
     * Check keystore health
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private Map<String, Object> checkKeystoreHealth() {
        try {
            String keystorePath = System.getProperty("server.ssl.key-store",
                                 System.getenv("SSL_KEYSTORE_PATH"));
            String keystorePassword = System.getProperty("server.ssl.key-store-password",
                                     System.getenv("SSL_KEYSTORE_PASSWORD"));

            // Rule #3: NO if-else, use Optional.flatMap() for null checks
            return java.util.Optional.ofNullable(keystorePath)
                .flatMap(path -> java.util.Optional.ofNullable(keystorePassword)
                    .map(password -> loadKeystoreDetails(path, password)))
                .orElseGet(() -> Map.of(
                    "keystore_valid", false,
                    "error", "Keystore path or password not configured",
                    "keystore_path", keystorePath != null ? keystorePath : "not_configured"
                ));

        } catch (Exception e) {
            log.warn("Keystore health check failed", e);
            return Map.of(
                "keystore_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Load keystore details
     */
    private Map<String, Object> loadKeystoreDetails(String keystorePath, String keystorePassword) {
        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            int certificateCount = keystore.size();
            String alias = keystore.aliases().nextElement();
            Certificate certificate = keystore.getCertificate(alias);

            return Map.of(
                "keystore_valid", true,
                "keystore_path", keystorePath,
                "certificate_count", certificateCount,
                "primary_alias", alias,
                "certificate_type", certificate.getType()
            );
        } catch (Exception e) {
            return Map.of(
                "keystore_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Check truststore health
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private Map<String, Object> checkTruststoreHealth() {
        try {
            String truststorePath = System.getProperty("server.ssl.trust-store",
                                   System.getenv("SSL_TRUSTSTORE_PATH"));
            String truststorePassword = System.getProperty("server.ssl.trust-store-password",
                                       System.getenv("SSL_TRUSTSTORE_PASSWORD"));

            // Rule #3: NO if-else, use Optional.flatMap() for null checks
            return java.util.Optional.ofNullable(truststorePath)
                .flatMap(path -> java.util.Optional.ofNullable(truststorePassword)
                    .map(password -> loadTruststoreDetails(path, password)))
                .orElseGet(() -> Map.of(
                    "truststore_valid", false,
                    "error", "Truststore path or password not configured",
                    "truststore_path", truststorePath != null ? truststorePath : "not_configured"
                ));

        } catch (Exception e) {
            log.warn("Truststore health check failed", e);
            return Map.of(
                "truststore_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Load truststore details
     */
    private Map<String, Object> loadTruststoreDetails(String truststorePath, String truststorePassword) {
        try {
            KeyStore truststore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(truststorePath)) {
                truststore.load(fis, truststorePassword.toCharArray());
            }

            int certificateCount = truststore.size();

            return Map.of(
                "truststore_valid", true,
                "truststore_path", truststorePath,
                "certificate_count", certificateCount
            );
        } catch (Exception e) {
            return Map.of(
                "truststore_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Check certificate expiration
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch expressions
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 5
     */
    private Map<String, Object> checkCertificateExpiration() {
        try {
            String keystorePath = System.getProperty("server.ssl.key-store",
                                 System.getenv("SSL_KEYSTORE_PATH"));
            String keystorePassword = System.getProperty("server.ssl.key-store-password",
                                     System.getenv("SSL_KEYSTORE_PASSWORD"));

            // Rule #3: NO if-else, use Optional.flatMap() for null checks
            return java.util.Optional.ofNullable(keystorePath)
                .flatMap(path -> java.util.Optional.ofNullable(keystorePassword)
                    .map(password -> checkCertificateDetails(path, password)))
                .orElseGet(() -> Map.of(
                    "certificate_valid", false,
                    "error", "Cannot check certificate expiration - keystore not configured"
                ));

        } catch (Exception e) {
            log.warn("Certificate expiration check failed", e);
            return Map.of(
                "certificate_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Check certificate details
     *
     * MANDATORY: Rule #14 - Pattern Matching with switch expression
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private Map<String, Object> checkCertificateDetails(String keystorePath, String keystorePassword) {
        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            String alias = keystore.aliases().nextElement();
            Certificate certificate = keystore.getCertificate(alias);

            // Rule #3: NO if-else, use Optional with instanceof check
            return java.util.Optional.of(certificate)
                .filter(X509Certificate.class::isInstance)
                .map(X509Certificate.class::cast)
                .map(x509cert -> buildCertificateExpirationDetails(x509cert))
                .orElseGet(() -> Map.of(
                    "certificate_valid", false,
                    "error", "Certificate is not X.509 format"
                ));

        } catch (Exception e) {
            return Map.of(
                "certificate_valid", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Build certificate expiration details
     *
     * MANDATORY: Rule #14 - Pattern Matching with switch expression
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    private Map<String, Object> buildCertificateExpirationDetails(X509Certificate x509cert) {
        Date expirationDate = x509cert.getNotAfter();
        Date currentDate = new Date();

        long daysUntilExpiration = (expirationDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24);
        boolean isExpired = currentDate.after(expirationDate);
        boolean isExpiringSoon = daysUntilExpiration <= CERTIFICATE_WARNING_DAYS;

        // Rule #14: Use switch expression for certificate status determination
        String status = switch (isExpired) {
            case true -> "EXPIRED";
            case false -> switch (isExpiringSoon) {
                case true -> "EXPIRING_SOON";
                case false -> "VALID";
            };
        };

        return Map.of(
            "certificate_valid", !isExpired,
            "certificate_status", status,
            "expiration_date", expirationDate.toString(),
            "days_until_expiration", daysUntilExpiration,
            "subject", x509cert.getSubjectDN().toString(),
            "issuer", x509cert.getIssuerDN().toString(),
            "serial_number", x509cert.getSerialNumber().toString(),
            "signature_algorithm", x509cert.getSigAlgName()
        );
    }

    /**
     * Check cipher suite configuration
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Stream API)
     * MANDATORY: Rule #13 - Stream API for collection processing
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private Map<String, Object> checkCipherSuiteConfiguration() {
        try {
            SSLContext sslContext = SSLContext.getDefault();
            SSLEngine sslEngine = sslContext.createSSLEngine();

            String[] enabledCipherSuites = sslEngine.getEnabledCipherSuites();
            String[] supportedCipherSuites = sslEngine.getSupportedCipherSuites();
            String[] enabledProtocols = sslEngine.getEnabledProtocols();
            String[] supportedProtocols = sslEngine.getSupportedProtocols();

            // Rule #13: NO for-loop, use Stream API with anyMatch for secure protocols
            boolean hasSecureProtocols = java.util.Arrays.stream(enabledProtocols)
                .anyMatch(protocol -> protocol.equals("TLSv1.2") || protocol.equals("TLSv1.3"));

            // Rule #13: NO for-loop, use Stream API with anyMatch for weak ciphers
            boolean hasWeakCiphers = java.util.Arrays.stream(enabledCipherSuites)
                .anyMatch(cipher -> cipher.contains("NULL") || cipher.contains("EXPORT") ||
                                   cipher.contains("DES") || cipher.contains("MD5"));

            return Map.of(
                "cipher_suites_secure", hasSecureProtocols && !hasWeakCiphers,
                "enabled_protocols", String.join(", ", enabledProtocols),
                "supported_protocols", String.join(", ", supportedProtocols),
                "enabled_cipher_count", enabledCipherSuites.length,
                "supported_cipher_count", supportedCipherSuites.length,
                "has_secure_protocols", hasSecureProtocols,
                "has_weak_ciphers", hasWeakCiphers
            );

        } catch (Exception e) {
            log.warn("Cipher suite configuration check failed", e);
            return Map.of(
                "cipher_suites_secure", false,
                "error", e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Get comprehensive SSL configuration status for load balancers
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Map<String, Object>> getSSLConfigurationStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Health health = health();
                
                return Map.of(
                    "service", "notification-service",
                    "ssl_health", health.getDetails(),
                    "ssl_status", health.getStatus().getCode(),
                    "load_balancer_ready", "UP".equals(health.getStatus().getCode()),
                    "timestamp", LocalDateTime.now().toString(),
                    "certificate_monitoring_enabled", true,
                    "tls_version_minimum", "1.2",
                    "cipher_suite_policy", "modern"
                );
                
            } catch (Exception e) {
                log.error("SSL configuration status check failed", e);
                return Map.of(
                    "service", "notification-service",
                    "ssl_status", "ERROR",
                    "load_balancer_ready", false,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
                );
            }
        });
    }
}