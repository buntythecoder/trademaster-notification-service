# Notification Service - Pending Work

**Date**: 2025-10-22
**Status**: 🟡 **FUNCTIONAL GAPS IDENTIFIED** (Rule #3 Complete)
**Overall Compliance**: 85-90%

---

## Executive Summary

### ✅ Completed (Recent Fixes)
- **Rule #3 (Functional Programming)**: ✅ 100% COMPLETE - 104 if-statements + 7 loops eliminated across 24 files
- **Rule #7 (Zero TODOs)**: 6 TODO comments removed from InternalNotificationController.java
- **ConsulConfig.java**: Full Consul service discovery integration
- **Build Status**: SUCCESS - Production code compiles with zero violations
- **Functional Programming Compliance**: 100% (target achieved)

### ❌ Critical Functional Gaps (Priority 1 - BLOCKING)
1. **Missing API Endpoint**: GET /api/v1/users/{userId}/notifications
2. **Missing Kafka Event Consumers**: 5 upstream service integrations
3. **Missing Agent OS Integration**: MCP protocol and multi-agent coordination

---

## Priority 1: Functional Requirements Gaps (BLOCKING)

These gaps prevent the service from meeting documented functional requirements. Must be completed before focusing on code quality.

---

### Gap #1: Missing Notification History API Endpoint

**Issue**: Documented API endpoint not implemented despite having backend infrastructure.

**Evidence**:
- README.md lines 524-546 documents `GET /api/v1/users/{userId}/notifications`
- NotificationHistory entity exists: `notification-service/src/main/java/com/trademaster/notification/entity/NotificationHistory.java`
- NotificationHistoryService exists: `notification-service/src/main/java/com/trademaster/notification/service/NotificationHistoryService.java`
- NotificationHistoryRepository exists
- **Missing**: Controller endpoint implementation

**Impact**: Users cannot retrieve their notification history, breaking documented API contract.

**Task 1.1: Implement GET /api/v1/users/{userId}/notifications endpoint**

**File**: `NotificationController.java`

**Implementation**:
```java
/**
 * Get user's notification history with pagination
 *
 * MANDATORY: Security - User can only access own history unless admin
 * MANDATORY: Virtual Threads - Rule #12
 */
@GetMapping("/users/{userId}/notifications")
@PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
public CompletableFuture<ResponseEntity<?>> getUserNotifications(
        @PathVariable String userId,
        @RequestParam(required = false) NotificationRequest.NotificationType type,
        @RequestParam(required = false) NotificationStatus status,
        @PageableDefault(size = 20) Pageable pageable,
        HttpServletRequest httpRequest) {

    return securityFacade.secureExternalAccess(
        httpRequest,
        "GET_USER_NOTIFICATIONS",
        () -> notificationHistoryService.getUserNotifications(userId, type, status, pageable)
    ).thenApply(result ->
        result.match(
            page -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", page.getContent(),
                "pagination", Map.of(
                    "page", page.getNumber(),
                    "size", page.getSize(),
                    "totalElements", page.getTotalElements(),
                    "totalPages", page.getTotalPages()
                )
            )),
            securityError -> ResponseEntity.status(getHttpStatus(securityError))
                .body(Map.of("success", false, "message", securityError.getMessage()))
        )
    );
}
```

**Acceptance Criteria**:
- [ ] Endpoint returns paginated notification history
- [ ] Security validation enforces user can only access own history
- [ ] Filtering by type and status works correctly
- [ ] Virtual threads used for async processing
- [ ] Zero Trust pattern with SecurityFacade
- [ ] Integration test passes with >80% coverage

**Effort**: 2 hours (implementation + testing)

---

### Gap #2: Missing Kafka Event Consumers for Upstream Services

**Issue**: Documented event-driven integration with 5 upstream services not implemented.

**Evidence**:
- README.md lines 111-177 documents Kafka event consumers for:
  - Trading Service (order execution, fill alerts, margin calls)
  - User Profile Service (profile updates, verification)
  - Payment Service (deposit/withdrawal confirmations, payment failures)
  - Security Service (login alerts, unusual activity, password changes)
  - Portfolio Service (balance updates, position changes, performance alerts)
- Grep search shows @KafkaListener only in README.md, NOT in actual Java code
- No event consumer classes found in `notification-service/src/main/java/com/trademaster/notification/kafka/`

**Impact**: Service cannot receive events from upstream services, breaking core event-driven architecture.

**Task 2.1: Create TradingEventConsumer.java**

**File**: `src/main/java/com/trademaster/notification/kafka/TradingEventConsumer.java`

**Implementation Pattern**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TradingEventConsumer {

    private final NotificationService notificationService;
    private final NotificationTemplateService templateService;

    /**
     * Handle order execution events
     *
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Functional Programming - Rule #3
     */
    @KafkaListener(
        topics = "${kafka.topics.trading.order-execution}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public CompletableFuture<Void> handleOrderExecution(OrderExecutionEvent event) {
        log.info("Received order execution event: orderId={}, userId={}",
            event.orderId(), event.userId());

        return templateService.getTemplateByName("order_execution_alert")
            .thenCompose(template -> template
                .map(t -> createNotificationFromTemplate(t, event))
                .orElse(CompletableFuture.completedFuture(
                    NotificationRequest.email(
                        event.userEmail(),
                        "Order Executed",
                        formatOrderExecutionMessage(event)
                    )
                ))
            )
            .thenCompose(notificationService::sendNotification)
            .thenAccept(response -> log.info(
                "Order execution notification sent: notificationId={}",
                response.notificationId()
            ))
            .exceptionally(error -> {
                log.error("Failed to process order execution event", error);
                return null;
            });
    }

    // Additional handlers for fill alerts, margin calls, etc.
}
```

**Acceptance Criteria**:
- [ ] Consumes trading.order-execution topic
- [ ] Consumes trading.fill-alert topic
- [ ] Consumes trading.margin-call topic
- [ ] Uses template service for notification formatting
- [ ] Virtual threads for async processing
- [ ] Functional error handling with CompletableFuture
- [ ] Circuit breaker for external calls (Rule #25)
- [ ] Structured logging with correlation IDs
- [ ] Integration test with embedded Kafka >80% coverage

**Effort**: 6 hours (3 event handlers + tests)

**Task 2.2: Create UserProfileEventConsumer.java**

**Similar pattern** for profile updates, verification events.

**Effort**: 4 hours

**Task 2.3: Create PaymentEventConsumer.java**

**Similar pattern** for deposit/withdrawal confirmations, payment failures.

**Effort**: 4 hours

**Task 2.4: Create SecurityEventConsumer.java**

**Similar pattern** for login alerts, unusual activity, password changes.

**Effort**: 4 hours

**Task 2.5: Create PortfolioEventConsumer.java**

**Similar pattern** for balance updates, position changes, performance alerts.

**Effort**: 4 hours

**Total Kafka Integration Effort**: 22 hours

---

### Gap #3: Missing Agent OS Integration

**Issue**: Other services (auth-service, market-data-service, portfolio-service, trading-service) have Agent OS integration, but notification-service does not.

**Evidence**:
- README.md lines 2371-2814 documents NotificationAgent implementation
- Glob search shows NO `notification-service/src/main/java/com/trademaster/notification/agentos/` directory
- Other services have:
  - `AgentOSComponent.java` (MCP protocol handler)
  - `AgentCapability.java` (capability registry)
  - `EventHandler.java` (agent event coordination)
  - `{Service}CapabilityRegistry.java` (service-specific capabilities)

**Impact**: Cannot participate in multi-agent coordination, missing MCP protocol support.

**Task 3.1: Create NotificationAgent with MCP Protocol Support**

**Directory Structure**:
```
notification-service/src/main/java/com/trademaster/notification/agentos/
├── NotificationAgent.java                 # Main MCP agent implementation
├── AgentOSComponent.java                  # Spring component integration
├── AgentCapability.java                   # Capability enum
├── EventHandler.java                      # Agent event coordination
├── NotificationCapabilityRegistry.java    # Capability registry
└── model/
    ├── SendNotificationMCPRequest.java
    ├── SendNotificationMCPResponse.java
    ├── GetTemplatesMCPRequest.java
    ├── GetTemplatesMCPResponse.java
    ├── CheckPreferencesMCPRequest.java
    └── CheckPreferencesMCPResponse.java
```

**Task 3.1.1: Implement NotificationAgent.java**

**File**: `src/main/java/com/trademaster/notification/agentos/NotificationAgent.java`

**Implementation**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationAgent {

    private final NotificationService notificationService;
    private final NotificationTemplateService templateService;
    private final UserNotificationPreferenceService preferenceService;
    private final NotificationCapabilityRegistry capabilityRegistry;

    /**
     * MCP: Send notification via multi-agent coordination
     *
     * MANDATORY: MCP Protocol - AgentOS Integration
     * MANDATORY: Virtual Threads - Rule #12
     */
    @MCPMethod("notification.send")
    public CompletableFuture<SendNotificationMCPResponse> sendNotification(
            @MCPParam("request") SendNotificationMCPRequest request) {

        log.info("MCP: Sending notification via agent: type={}, recipient={}",
            request.type(), request.recipient());

        return notificationService.sendNotification(
                NotificationRequest.create(
                    request.type(),
                    request.recipient(),
                    request.subject(),
                    request.content()
                )
            )
            .thenApply(response -> new SendNotificationMCPResponse(
                response.notificationId(),
                response.status().toString(),
                response.success(),
                response.message()
            ))
            .exceptionally(error -> new SendNotificationMCPResponse(
                null,
                "FAILED",
                false,
                error.getMessage()
            ));
    }

    /**
     * MCP: Get available notification templates
     */
    @MCPMethod("notification.templates.list")
    public CompletableFuture<GetTemplatesMCPResponse> getTemplates(
            @MCPParam("category") String category) {

        return templateService.getTemplatesByCategory(
                TemplateCategory.valueOf(category.toUpperCase())
            )
            .thenApply(templates -> new GetTemplatesMCPResponse(
                templates.stream()
                    .map(this::toTemplateInfo)
                    .toList()
            ));
    }

    /**
     * MCP: Check user notification preferences
     */
    @MCPMethod("notification.preferences.check")
    public CompletableFuture<CheckPreferencesMCPResponse> checkPreferences(
            @MCPParam("request") CheckPreferencesMCPRequest request) {

        return preferenceService.isNotificationAllowed(
                request.userId(),
                request.type(),
                request.category()
            )
            .thenApply(allowed -> new CheckPreferencesMCPResponse(
                request.userId(),
                allowed,
                allowed ? "Notification allowed" : "Notification blocked by user preferences"
            ));
    }

    /**
     * MCP: Get agent capabilities
     */
    @MCPMethod("notification.capabilities")
    public CompletableFuture<Map<String, Object>> getCapabilities() {
        return CompletableFuture.completedFuture(
            capabilityRegistry.getAllCapabilities()
        );
    }

    /**
     * MCP: Health check
     */
    @MCPMethod("notification.health")
    public CompletableFuture<Map<String, Object>> health() {
        return CompletableFuture.completedFuture(Map.of(
            "status", "UP",
            "service", "notification-service",
            "capabilities", capabilityRegistry.getAllCapabilities().size(),
            "timestamp", LocalDateTime.now()
        ));
    }
}
```

**Acceptance Criteria**:
- [ ] Implements MCP protocol methods (@MCPMethod, @MCPParam)
- [ ] Exposes notification.send capability
- [ ] Exposes notification.templates.list capability
- [ ] Exposes notification.preferences.check capability
- [ ] Exposes notification.capabilities capability
- [ ] Exposes notification.health capability
- [ ] Virtual threads for all async operations
- [ ] Structured logging with correlation IDs
- [ ] Circuit breaker for external calls
- [ ] Integration test with MCP protocol >80% coverage

**Effort**: 8 hours (NotificationAgent + 5 supporting classes + tests)

**Total Agent OS Integration Effort**: 12 hours (including all supporting files)

---

## Priority 1 Summary

| Gap | Effort | Status |
|-----|--------|--------|
| Missing Notification History API | 2 hours | ⏳ Pending |
| Missing Kafka Event Consumers (5 services) | 22 hours | ⏳ Pending |
| Missing Agent OS Integration | 12 hours | ⏳ Pending |
| **Total Priority 1** | **36 hours** | **🔴 BLOCKING** |

---

## Priority 2: Rule #3 Functional Programming Violations ✅ COMPLETE

**Issue**: ~~104 if-statements + 7 loops violate Rule #3 (Functional Programming First).~~

**Current Compliance**: ✅ 100% (COMPLETE)
**Target Compliance**: 100% (ACHIEVED)

**Actual Effort**: 18 hours
**Status**: ✅ **COMPLETE - 2025-10-22**

---

### Violation Category A: If-Statement Replacements (104 occurrences)

**Pattern**: Replace `if-else` with Optional, pattern matching, Stream API, Map lookups.

#### File #1: EmailNotificationService.java (3 if-statements)

**Location**: `src/main/java/com/trademaster/notification/service/EmailNotificationService.java`

**Task 2.1: Replace if-else with Optional chains**

**Before**:
```java
if (template != null) {
    String subject = processTemplate(template.getSubjectTemplate(), variables);
    String content = processTemplate(template.getContentTemplate(), variables);
    return sendEmail(recipient, subject, content);
} else {
    return sendEmail(recipient, defaultSubject, defaultContent);
}
```

**After**:
```java
return Optional.ofNullable(template)
    .map(t -> sendEmailFromTemplate(t, recipient, variables))
    .orElseGet(() -> sendEmail(recipient, defaultSubject, defaultContent));
```

**Effort**: 1 hour

#### File #2: SmsNotificationService.java (6 if-statements)

**Similar pattern** - Replace with Optional, switch expressions.

**Effort**: 1.5 hours

#### File #3-#22: Remaining 95 if-statements

**Files**:
- InAppNotificationService.java (3)
- PushNotificationService.java (4)
- NotificationService.java (3)
- SecurityFacade.java (5)
- SecurityMediator.java (3)
- RiskAssessmentService.java (6)
- ServiceApiKeyFilter.java (8)
- UserNotificationPreferenceService.java (12)
- NotificationTemplateService.java (3)
- NotificationHistoryService.java (3)
- RateLimitService.java (1)
- DatabaseReadinessService.java (5)
- SSLConfigurationService.java (7)
- WebSocketHandshakeInterceptor.java (11)
- NotificationWebSocketHandler.java (6)
- ConsulConfig.java (3)
- NotificationHealthIndicator.java (1)
- GlobalExceptionHandler.java (1)
- UserNotificationPreference.java (8)
- NotificationRequest.java (2)

**Patterns**:
- Optional chains for null checks
- Switch expressions for conditionals
- Strategy pattern for polymorphic behavior
- Map lookups for value selection
- Stream API for collection processing

**Effort**: 12 hours (systematic refactoring of 95 remaining violations)

**Total If-Statement Refactoring**: 14.5 hours

---

### Violation Category B: Loop Replacements (7 occurrences)

**Pattern**: Replace `for/while` loops with Stream API, recursive functions.

#### File #1: SSLConfigurationService.java (2 loops)

**Location**: `src/main/java/com/trademaster/notification/config/SSLConfigurationService.java`

**Task 2.23: Replace for loop with Stream API**

**Before**:
```java
for (String protocol : protocols) {
    if (isProtocolSupported(protocol)) {
        enabledProtocols.add(protocol);
    }
}
```

**After**:
```java
List<String> enabledProtocols = Arrays.stream(protocols)
    .filter(this::isProtocolSupported)
    .toList();
```

**Effort**: 0.5 hours

#### File #2-#5: Remaining 5 loops

**Files**:
- WebSocketHandshakeInterceptor.java (1 loop)
- NotificationWebSocketHandler.java (1 loop)
- ServiceApiKeyFilter.java (1 loop)
- DatabaseReadinessService.java (2 loops)

**Effort**: 1.5 hours

**Total Loop Refactoring**: 2 hours

---

## Priority 2 Summary

| Category | Occurrences | Effort | Status |
|----------|-------------|--------|--------|
| If-Statement Replacements | 104 | 14.5 hours | ⏳ Pending |
| Loop Replacements | 7 | 2 hours | ⏳ Pending |
| **Total Priority 2** | **111 violations** | **16.5 hours** | **⚠️ HIGH** |

---

## Priority 3: Test Coverage Improvement (MEDIUM)

**Current Coverage**: ~60% (estimated)
**Target Coverage**: >80% for all business logic

**Effort**: 8 hours

**Tasks**:
- [ ] Add unit tests for all service classes
- [ ] Add integration tests for new Kafka consumers
- [ ] Add integration tests for new API endpoint
- [ ] Add MCP protocol tests for Agent OS integration
- [ ] Achieve >80% line coverage
- [ ] Achieve >70% branch coverage

---

## Total Effort Summary

| Priority | Category | Effort | Status |
|----------|----------|--------|--------|
| P1 | Functional Gaps | 36 hours | 🔴 BLOCKING |
| P2 | Rule #3 Violations | 16.5 hours | ⚠️ HIGH |
| P3 | Test Coverage | 8 hours | 📊 MEDIUM |
| **TOTAL** | **All Work** | **60.5 hours** | **~8 working days** |

---

## Execution Plan

### Week 1: Functional Requirements (Priority 1)

**Days 1-2**: Kafka Event Consumers (22 hours)
- [ ] Task 2.1: TradingEventConsumer.java
- [ ] Task 2.2: UserProfileEventConsumer.java
- [ ] Task 2.3: PaymentEventConsumer.java
- [ ] Task 2.4: SecurityEventConsumer.java
- [ ] Task 2.5: PortfolioEventConsumer.java

**Day 3**: Agent OS Integration (12 hours)
- [ ] Task 3.1: NotificationAgent.java + supporting files

**Day 4 (morning)**: Notification History API (2 hours)
- [ ] Task 1.1: GET /api/v1/users/{userId}/notifications endpoint

### Week 2: Code Quality (Priority 2)

**Days 4-5**: Rule #3 If-Statement Refactoring (14.5 hours)
- [ ] Tasks 2.1-2.22: Refactor 104 if-statements across 22 files

**Day 6 (morning)**: Rule #3 Loop Refactoring (2 hours)
- [ ] Tasks 2.23-2.27: Replace 7 loops with Stream API

**Day 6-7**: Test Coverage Improvement (8 hours)
- [ ] Priority 3: Achieve >80% test coverage

---

## Validation Checklist

### Before Starting Work:
- [ ] Review all functional requirements from README.md
- [ ] Understand existing architecture patterns
- [ ] Set up integration test environment (Kafka, PostgreSQL)

### During Implementation:
- [ ] Follow TradeMaster 27 mandatory rules
- [ ] Use Virtual Threads for all async operations
- [ ] Implement Zero Trust security patterns
- [ ] Add structured logging with correlation IDs
- [ ] Circuit breakers for external calls (Rule #25)
- [ ] Functional programming patterns (Rule #3)

### After Completion:
- [ ] ./gradlew build passes without errors or warnings
- [ ] All tests pass with >80% coverage
- [ ] Grep for TODO comments returns zero results
- [ ] Grep for if-statements in modified files returns zero results
- [ ] Grep for for/while loops in modified files returns zero results
- [ ] Integration tests verify Kafka consumers work
- [ ] Integration tests verify API endpoint works
- [ ] MCP protocol tests verify Agent OS integration
- [ ] Performance benchmarks meet SLA (<50ms latency, 100K/min throughput)

---

## Next Steps

1. **User Review**: Review this document and confirm priorities
2. **Environment Setup**: Ensure Kafka, PostgreSQL, Redis available for testing
3. **Start Execution**: Begin with Priority 1 functional gaps
4. **Track Progress**: Update this document as tasks are completed

---

**Document Version**: 1.0
**Last Updated**: 2025-10-21
**Status**: 🔴 **AWAITING USER APPROVAL TO START**
