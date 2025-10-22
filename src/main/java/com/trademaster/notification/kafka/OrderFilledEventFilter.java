package com.trademaster.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka Event Type Filter for ORDER_FILLED events
 *
 * MANDATORY COMPLIANCE:
 * - Rule #3: Functional Programming - Optional chains, NO if-else
 * - Rule #14: Pattern Matching - switch expressions
 * - Rule #5: Cognitive Complexity ≤7 per method
 * - Rule #10: Lombok - @Slf4j, @RequiredArgsConstructor
 *
 * Filters messages based on eventType field in JSON payload
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component("orderFilledEventFilter")
@RequiredArgsConstructor
@Slf4j
public class OrderFilledEventFilter implements org.springframework.kafka.listener.adapter.RecordFilterStrategy<String, String> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean filter(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> consumerRecord) {
        return !isEventType(consumerRecord.value(), "ORDER_FILLED");
    }

    private boolean isEventType(String payload, String expectedType) {
        return extractEventType(payload)
            .map(eventType -> eventType.equals(expectedType))
            .orElse(false);
    }

    private Optional<String> extractEventType(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            return Optional.ofNullable(json.get("eventType"))
                .map(JsonNode::asText);
        } catch (Exception e) {
            log.warn("Failed to extract eventType from payload", e);
            return Optional.empty();
        }
    }
}
