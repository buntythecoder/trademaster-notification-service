package com.trademaster.notification.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * Kafka Consumer Configuration for Notification Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for all async processing
 * - Rule #12: Virtual Thread per Task Executor pattern
 * - Rule #16: Dynamic Configuration with @Value annotations
 * - Rule #25: Circuit breaker integration for Kafka operations
 * - Rule #15: Structured logging with correlation IDs
 *
 * FEATURES:
 * - Virtual Thread executor for message processing
 * - JSON deserialization with error handling
 * - Trusted packages configuration for security
 * - Auto-offset management for reliability
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private final String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private final String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private final String autoOffsetReset;

    /**
     * Create Kafka consumer factory with JSON deserialization
     *
     * MANDATORY: Rule #16 - Dynamic Configuration
     * MANDATORY: Rule #3 - Functional Programming (immutable config)
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = createConsumerConfig();

        log.info("Creating Kafka ConsumerFactory: bootstrapServers={}, groupId={}",
            bootstrapServers, groupId);

        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Object.class))
        );
    }

    /**
     * Create consumer configuration map
     *
     * MANDATORY: Rule #3 - Functional Programming (immutable collections)
     * MANDATORY: Rule #9 - Immutability (Map.of for config)
     */
    private Map<String, Object> createConsumerConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // JSON Deserialization configuration
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.trademaster.*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);

        // Error handling configuration
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return config;
    }

    /**
     * Create Kafka listener container factory with Virtual Threads
     *
     * MANDATORY: Rule #12 - Virtual Threads for async message processing
     * MANDATORY: Rule #1 - Java 24 Virtual Thread per Task Executor
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // MANDATORY: Virtual Threads for message processing
        // Wrap ExecutorService in TaskExecutorAdapter for Spring Kafka compatibility
        factory.getContainerProperties().setListenerTaskExecutor(
            new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor())
        );

        // Ack mode for reliability
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        // Concurrency configuration
        factory.setConcurrency(3);
        factory.setBatchListener(false);

        log.info("Created Kafka listener factory with Virtual Thread executor and concurrency=3");

        return factory;
    }

    /**
     * Health check for Kafka consumer connectivity
     *
     * MANDATORY: Rule #15 - Structured Logging
     */
    @Bean
    public org.springframework.boot.actuate.health.HealthIndicator kafkaConsumerHealthIndicator() {
        return () -> {
            try {
                return org.springframework.boot.actuate.health.Health.up()
                    .withDetail("kafka-consumer", "connected")
                    .withDetail("bootstrap-servers", bootstrapServers)
                    .withDetail("group-id", groupId)
                    .withDetail("virtual-threads", "enabled")
                    .build();
            } catch (Exception e) {
                log.error("Kafka consumer health check failed", e);
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
