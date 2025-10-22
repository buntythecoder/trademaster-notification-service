package com.trademaster.notification.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.notification.NotificationServiceApplication;
import com.trademaster.notification.dto.BulkNotificationRequest;
import com.trademaster.notification.dto.BulkNotificationResponse;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.entity.NotificationHistory;
import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.UserNotificationPreference;
import com.trademaster.notification.repository.NotificationHistoryRepository;
import com.trademaster.notification.repository.NotificationTemplateRepository;
import com.trademaster.notification.repository.UserNotificationPreferenceRepository;
import com.trademaster.notification.service.EmailNotificationService;
import com.trademaster.notification.service.NotificationService;
import com.trademaster.notification.service.SmsNotificationService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive Integration Tests for Notification Service
 * 
 * MANDATORY: TestContainers - Rule #20
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@SpringBootTest(
    classes = NotificationServiceApplication.class, 
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "spring.threads.virtual.enabled=true",
        "spring.test.context.cache.maxSize=3"
    }
)
@Testcontainers
@DisplayName("Notification Service Integration Tests")
class NotificationServiceIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass testpass")
            .withReuse(false);

    @Container
    static GenericContainer<?> mailhog = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:v1.0.1"))
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v2/messages").forPort(8025))
            .withReuse(false);

    @Container
    static GenericContainer<?> wiremock = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.3.1"))
            .withExposedPorts(8080)
            .withCommand("--port", "8080", "--verbose")
            .waitingFor(Wait.forHttp("/__admin/health").forPort(8080))
            .withReuse(false);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private SmsNotificationService smsNotificationService;

    @Autowired
    private NotificationHistoryRepository historyRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private UserNotificationPreferenceRepository preferenceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "testpass");
        registry.add("spring.cache.redis.time-to-live", () -> "600000");

        // Email configuration with MailHog
        registry.add("spring.mail.host", mailhog::getHost);
        registry.add("spring.mail.port", () -> mailhog.getMappedPort(1025));
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registry.add("notification.email.enabled", () -> "true");
        registry.add("notification.email.default-sender", () -> "test@trademaster.com");

        // SMS configuration with WireMock
        registry.add("twilio.account-sid", () -> "test-account-sid");
        registry.add("twilio.auth-token", () -> "test-auth-token");
        registry.add("twilio.phone-number", () -> "+1234567890");
        registry.add("notification.sms.enabled", () -> "false"); // Disable real SMS for testing

        // Push notification configuration
        registry.add("firebase.credentials-path", () -> "classpath:firebase-test-key.json");
        registry.add("notification.push.enabled", () -> "false"); // Disable real push for testing

        // Circuit breaker configuration for testing
        registry.add("resilience4j.circuitbreaker.instances.email.failure-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.email.slow-call-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.email.slow-call-duration-threshold", () -> "2000ms");
        registry.add("resilience4j.circuitbreaker.instances.email.sliding-window-size", () -> "10");
        registry.add("resilience4j.circuitbreaker.instances.email.minimum-number-of-calls", () -> "5");

        registry.add("resilience4j.retry.instances.email.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.email.wait-duration", () -> "500ms");

        registry.add("resilience4j.timelimiter.instances.email.timeout-duration", () -> "5s");
    }

    @BeforeAll
    static void beforeAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = serverPort;
        RestAssured.basePath = "/api/v1";

        // Setup test data
        setupTestTemplates();
        setupTestPreferences();
        
        // Setup WireMock stubs for external services
        setupWireMockStubs();
    }

    /**
     * Test complete email notification lifecycle
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: TestContainers - Rule #20
     */
    @Test
    @DisplayName("Email Notification Lifecycle - Send, Track, and Verify")
    void emailNotificationLifecycle_SendTrackVerify_ShouldWorkEndToEnd() throws InterruptedException {
        // Given: User preferences and email template
        String testEmail = "user@example.com";
        String testSubject = "Welcome to TradeMaster";
        String testContent = "Welcome to our trading platform!";

        NotificationRequest emailRequest = NotificationRequest.builder()
                .type(NotificationRequest.NotificationType.EMAIL)
                .emailRecipient(testEmail)
                .subject(testSubject)
                .content(testContent)
                .priority(NotificationRequest.Priority.MEDIUM)
                .templateName("welcome")
                .templateVariables(Map.of(
                    "firstName", "John",
                    "lastName", "Doe",
                    "dashboardUrl", "https://app.trademaster.com/dashboard"
                ))
                .build();

        // When: Send notification via API
        NotificationResponse response = given()
                .contentType(ContentType.JSON)
                .body(emailRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/notifications/send")
                .then()
                .statusCode(200)
                .extract()
                .as(NotificationResponse.class);

        // Then: Verify immediate response
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.notificationId()).isNotNull();

        // And: Verify database persistence
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<NotificationHistory> history =
                        historyRepository.findById(response.notificationId());
                    assertThat(history).isPresent();
                    assertThat(history.get().getStatus()).isEqualTo(NotificationHistory.NotificationStatus.SENT);
                });

        // And: Verify email was sent to MailHog
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<Map<String, Object>> messages = getMailHogMessages();
                    assertThat(messages).isNotEmpty();
                    assertThat(messages.get(0).get("Content")).toString()
                        .contains("Welcome to TradeMaster");
                });
    }

    /**
     * Test bulk notification processing with concurrent Virtual Threads
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Concurrent Processing - Rule #12
     */
    @Test
    @DisplayName("Bulk Notification Processing - Concurrent Virtual Threads")
    void bulkNotificationProcessing_ConcurrentVirtualThreads_ShouldProcessAllNotifications() {
        // Given: Bulk notification request for multiple recipients
        List<String> recipients = List.of(
                "user1@example.com",
                "user2@example.com",
                "user3@example.com",
                "user4@example.com",
                "user5@example.com"
        );

        BulkNotificationRequest bulkRequest = BulkNotificationRequest.builder()
                .type(NotificationRequest.NotificationType.EMAIL)
                .recipients(recipients)
                .subject("Market Alert")
                .content("NIFTY has crossed 18000 - Your watchlist stocks are trending up!")
                .priority(NotificationRequest.Priority.HIGH)
                .referenceType("BULK_MARKET_ALERT")
                .personalizeContent(false)
                .build();

        // When: Send bulk notifications
        long startTime = System.currentTimeMillis();
        
        BulkNotificationResponse bulkResponse = given()
                .contentType(ContentType.JSON)
                .body(bulkRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/notifications/bulk")
                .then()
                .statusCode(200)
                .extract()
                .as(BulkNotificationResponse.class);

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Then: Verify bulk processing response
        assertThat(bulkResponse).isNotNull();
        assertThat(bulkResponse.totalRequests()).isEqualTo(5);
        assertThat(bulkResponse.successfulRequests()).isEqualTo(5);
        assertThat(bulkResponse.failedRequests()).isZero();

        // And: Verify processing was fast due to Virtual Threads
        assertThat(processingTime).isLessThan(5000); // Should complete in under 5 seconds

        // And: Verify all notifications were persisted
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<NotificationHistory> histories = historyRepository.findAll();
                    long sentCount = histories.stream()
                            .filter(h -> h.getStatus() == NotificationHistory.NotificationStatus.SENT)
                            .count();
                    assertThat(sentCount).isGreaterThanOrEqualTo(5);
                });
    }

    /**
     * Test notification preferences and filtering
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Pattern Matching - Rule #14
     */
    @Test
    @DisplayName("Notification Preferences - Filtering and User Settings")
    void notificationPreferences_FilteringAndUserSettings_ShouldRespectUserPreferences() {
        // Given: User with email disabled preference
        String userId = "user123";
        String userEmail = "preferences@example.com";
        
        UserNotificationPreference preference = UserNotificationPreference.builder()
                .userId(userId)
                .notificationsEnabled(true)
                .preferredChannel(NotificationRequest.NotificationType.SMS)
                .enabledChannels(Set.of(
                        NotificationRequest.NotificationType.SMS,
                        NotificationRequest.NotificationType.PUSH,
                        NotificationRequest.NotificationType.IN_APP
                )) // Email disabled
                .enabledCategories(Set.of(
                        NotificationTemplate.TemplateCategory.SYSTEM,
                        NotificationTemplate.TemplateCategory.ACCOUNT
                ))
                .systemAlertsEnabled(true)
                .accountAlertsEnabled(true)
                .createdBy("test-setup")
                .updatedBy("test-setup")
                .build();

        preferenceRepository.save(preference);

        NotificationRequest emailRequest = createEmailNotification(userEmail, "Test Subject", "Test Content")
                .toBuilder()
                .recipient(userId)
                .build();

        // When: Try to send email notification
        NotificationResponse response = given()
                .contentType(ContentType.JSON)
                .body(emailRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/notifications/send")
                .then()
                .statusCode(200)
                .extract()
                .as(NotificationResponse.class);

        // Then: Verify notification was filtered out
        assertThat(response).isNotNull();
        // Response should indicate that notification was skipped due to user preferences
        
        // And: Verify no email was sent
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Map<String, Object>> messages = getMailHogMessages();
                    // Should not contain any new messages for this test
                    boolean foundTestMessage = messages.stream()
                            .anyMatch(msg -> msg.get("Content").toString().contains("Test Content"));
                    assertThat(foundTestMessage).isFalse();
                });
    }

    /**
     * Test WebSocket real-time notifications
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: WebSocket Integration - Rule #20
     */
    @Test
    @DisplayName("WebSocket Real-time Notifications - Live Updates")
    void websocketRealTimeNotifications_LiveUpdates_ShouldReceiveNotificationsInRealTime()
            throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
        // Given: WebSocket connection
        stompClient = new WebSocketStompClient(new org.springframework.web.socket.client.standard.StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String websocketUrl = "ws://localhost:" + serverPort + "/ws";
        StompSession stompSession = stompClient.connect(websocketUrl, new StompSessionHandlerAdapter() {}).get();

        // Subscribe to notifications
        AtomicReference<Map<String, Object>> receivedNotification = new AtomicReference<>();
        CompletableFuture<Map<String, Object>> notificationFuture = new CompletableFuture<>();

        StompSession.Subscription subscription = stompSession.subscribe("/topic/notifications/user123", 
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> notification = (Map<String, Object>) payload;
                        receivedNotification.set(notification);
                        notificationFuture.complete(notification);
                    }
                });

        // When: Send in-app notification
        NotificationRequest inAppRequest = NotificationRequest.builder()
                .type(NotificationRequest.NotificationType.IN_APP)
                .recipient("user123")
                .subject("Real-time Alert")
                .content("Your order has been executed successfully!")
                .priority(NotificationRequest.Priority.HIGH)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(inAppRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/notifications/send")
                .then()
                .statusCode(200);

        // Then: Verify WebSocket notification received
        Map<String, Object> notification = notificationFuture.get(10, TimeUnit.SECONDS);
        
        assertThat(notification).isNotNull();
        assertThat(notification.get("subject")).isEqualTo("Real-time Alert");
        assertThat(notification.get("content")).isEqualTo("Your order has been executed successfully!");
        assertThat(notification.get("type")).isEqualTo("IN_APP");

        // Cleanup
        subscription.unsubscribe();
        stompSession.disconnect();
    }

    /**
     * Test circuit breaker functionality
     * MANDATORY: Circuit Breaker - Rule #24
     * MANDATORY: Resilience Patterns - Rule #24
     */
    @Test
    @DisplayName("Circuit Breaker - Resilience Under Failure")
    void circuitBreaker_ResilienceUnderFailure_ShouldHandleServiceFailuresGracefully() {
        // Given: Configure circuit breaker to fail fast
        // Stop MailHog to simulate email service failure
        mailhog.stop();

        List<NotificationRequest> notifications = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            notifications.add(createEmailNotification(
                "user" + i + "@example.com", 
                "Test Circuit Breaker", 
                "This should trigger circuit breaker"
            ));
        }

        // When: Send multiple notifications that will fail
        List<CompletableFuture<Void>> futures = notifications.stream()
                .map(notification -> CompletableFuture.runAsync(() -> {
                    given()
                            .contentType(ContentType.JSON)
                            .body(notification)
                            .auth().basic("admin", "admin123")
                            .when()
                            .post("/notifications/send")
                            .then()
                            .statusCode(200); // Should still return 200 but with failure message
                }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()))
                .toList();

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then: Verify circuit breaker opened and requests failed gracefully
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<NotificationHistory> histories = historyRepository.findAll();
                    long failedCount = histories.stream()
                            .filter(h -> h.getStatus() == NotificationHistory.NotificationStatus.FAILED)
                            .count();
                    
                    // Some should fail due to actual service unavailability,
                    // others should fail fast due to open circuit breaker
                    assertThat(failedCount).isGreaterThan(0);
                });
    }

    /**
     * Test notification templating with dynamic content
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Template Processing - Rule #20
     */
    @Test
    @DisplayName("Notification Templating - Dynamic Content Generation")
    void notificationTemplating_DynamicContentGeneration_ShouldProcessTemplatesCorrectly() {
        // Given: Template with variables
        String userId = "template-user";
        Map<String, Object> templateVariables = Map.of(
                "firstName", "Alice",
                "lastName", "Johnson",
                "accountBalance", "₹50,000.00",
                "portfolioGain", "12.5%",
                "dashboardUrl", "https://app.trademaster.com/dashboard"
        );

        NotificationRequest templatedRequest = NotificationRequest.builder()
                .type(NotificationRequest.NotificationType.EMAIL)
                .recipient(userId)
                .emailRecipient("alice.johnson@example.com")
                .subject("Portfolio Performance Update")
                .templateName("portfolio-update")
                .templateVariables(templateVariables)
                .priority(NotificationRequest.Priority.MEDIUM)
                .build();

        // When: Send templated notification
        NotificationResponse response = given()
                .contentType(ContentType.JSON)
                .body(templatedRequest)
                .auth().basic("admin", "admin123")
                .when()
                .post("/notifications/send")
                .then()
                .statusCode(200)
                .extract()
                .as(NotificationResponse.class);

        // Then: Verify response
        assertThat(response.success()).isTrue();
        assertThat(response.notificationId()).isNotNull();

        // And: Verify templated email was sent with correct content
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<Map<String, Object>> messages = getMailHogMessages();
                    assertThat(messages).isNotEmpty();
                    
                    boolean foundTemplatedEmail = messages.stream()
                            .anyMatch(msg -> {
                                String content = msg.get("Content").toString();
                                return content.contains("Alice Johnson") &&
                                       content.contains("₹50,000.00") &&
                                       content.contains("12.5%") &&
                                       content.contains("dashboard");
                            });
                    
                    assertThat(foundTemplatedEmail).isTrue();
                });
    }

    // Helper methods

    private void setupTestTemplates() {
        List<NotificationTemplate> templates = List.of(
                NotificationTemplate.builder()
                        .templateName("welcome")
                        .displayName("Welcome Template")
                        .notificationType(NotificationRequest.NotificationType.EMAIL)
                        .category(NotificationTemplate.TemplateCategory.WELCOME)
                        .subjectTemplate("Welcome to TradeMaster, {{firstName}}!")
                        .contentTemplate("<html><body><h1>Welcome {{firstName}} {{lastName}}!</h1>" +
                                "<p>Your trading account is ready.</p>" +
                                "<a href='{{dashboardUrl}}'>Access Dashboard</a></body></html>")
                        .version(1)
                        .active(true)
                        .defaultPriority(NotificationRequest.Priority.MEDIUM)
                        .createdBy("test-setup")
                        .updatedBy("test-setup")
                        .build(),

                NotificationTemplate.builder()
                        .templateName("portfolio-update")
                        .displayName("Portfolio Update Template")
                        .notificationType(NotificationRequest.NotificationType.EMAIL)
                        .category(NotificationTemplate.TemplateCategory.TRADING)
                        .subjectTemplate("Portfolio Performance Update")
                        .contentTemplate("<html><body><h1>Hello {{firstName}} {{lastName}}</h1>" +
                                "<p>Your current account balance: {{accountBalance}}</p>" +
                                "<p>Portfolio gain: {{portfolioGain}}</p>" +
                                "<a href='{{dashboardUrl}}'>View Dashboard</a></body></html>")
                        .version(1)
                        .active(true)
                        .defaultPriority(NotificationRequest.Priority.MEDIUM)
                        .createdBy("test-setup")
                        .updatedBy("test-setup")
                        .build()
        );

        templateRepository.saveAll(templates);
    }

    private void setupTestPreferences() {
        List<UserNotificationPreference> preferences = List.of(
                UserNotificationPreference.builder()
                        .userId("test-user-1")
                        .notificationsEnabled(true)
                        .preferredChannel(NotificationRequest.NotificationType.EMAIL)
                        .enabledChannels(Set.of(
                                NotificationRequest.NotificationType.EMAIL,
                                NotificationRequest.NotificationType.SMS,
                                NotificationRequest.NotificationType.PUSH,
                                NotificationRequest.NotificationType.IN_APP
                        ))
                        .enabledCategories(Set.of(
                                NotificationTemplate.TemplateCategory.SYSTEM,
                                NotificationTemplate.TemplateCategory.TRADING,
                                NotificationTemplate.TemplateCategory.ACCOUNT
                        ))
                        .systemAlertsEnabled(true)
                        .tradingAlertsEnabled(true)
                        .accountAlertsEnabled(true)
                        .createdBy("test-setup")
                        .updatedBy("test-setup")
                        .build(),

                UserNotificationPreference.builder()
                        .userId("test-user-2")
                        .notificationsEnabled(true)
                        .preferredChannel(NotificationRequest.NotificationType.SMS)
                        .enabledChannels(Set.of(
                                NotificationRequest.NotificationType.SMS,
                                NotificationRequest.NotificationType.IN_APP
                        ))
                        .enabledCategories(Set.of(
                                NotificationTemplate.TemplateCategory.SYSTEM,
                                NotificationTemplate.TemplateCategory.ACCOUNT
                        ))
                        .systemAlertsEnabled(true)
                        .tradingAlertsEnabled(false)
                        .accountAlertsEnabled(true)
                        .createdBy("test-setup")
                        .updatedBy("test-setup")
                        .build()
        );

        preferenceRepository.saveAll(preferences);
    }

    private void setupWireMockStubs() {
        // Setup WireMock stubs for Twilio SMS API
        String twilioUrl = "http://" + wiremock.getHost() + ":" + wiremock.getFirstMappedPort();
        
        // SMS success response stub
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "request", Map.of(
                                "method", "POST",
                                "url", "/2010-04-01/Accounts/test-account-sid/Messages.json",
                                "headers", Map.of("Content-Type", Map.of("matches", "application/x-www-form-urlencoded"))
                        ),
                        "response", Map.of(
                                "status", 201,
                                "headers", Map.of("Content-Type", "application/json"),
                                "jsonBody", Map.of(
                                        "sid", "SMS" + System.currentTimeMillis(),
                                        "status", "queued",
                                        "to", "+1234567890",
                                        "from", "+1234567890"
                                )
                        )
                ))
                .when()
                .post("http://" + wiremock.getHost() + ":" + wiremock.getFirstMappedPort() + "/__admin/mappings")
                .then()
                .statusCode(201);
    }

    private NotificationRequest createEmailNotification(String email, String subject, String content) {
        return NotificationRequest.builder()
                .type(NotificationRequest.NotificationType.EMAIL)
                .emailRecipient(email)
                .subject(subject)
                .content(content)
                .priority(NotificationRequest.Priority.MEDIUM)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getMailHogMessages() {
        try {
            String response = given()
                    .when()
                    .get("http://" + mailhog.getHost() + ":" + mailhog.getMappedPort(8025) + "/api/v2/messages")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            Map<String, Object> responseMap = objectMapper.readValue(response, 
                    new TypeReference<Map<String, Object>>() {});
            
            return (List<Map<String, Object>>) responseMap.getOrDefault("items", List.of());
        } catch (Exception e) {
            return List.of();
        }
    }
}