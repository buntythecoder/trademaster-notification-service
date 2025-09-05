# TradeMaster Notification Service Production Docker Image
# MANDATORY: Java 24 Virtual Threads
# MANDATORY: Production Security Standards
# MANDATORY: Multi-stage Build for Optimization

# Build stage
FROM openjdk:24-jdk-slim AS builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle/
COPY build.gradle ./
COPY settings.gradle ./

# Copy source code
COPY src ./src/

# Make Gradle wrapper executable
RUN chmod +x ./gradlew

# Build application with Java 24 preview features
RUN ./gradlew build -x test --no-daemon

# Production stage
FROM openjdk:24-jdk-slim AS production

# Create non-root user for security
RUN groupadd -r trademaster && useradd -r -g trademaster -u 1000 trademaster

# Install runtime dependencies and security updates
RUN apt-get update && apt-get install -y \
    curl \
    netcat-traditional \
    dumb-init \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p /var/log/notification-service \
    && mkdir -p /tmp/notification-service \
    && mkdir -p /etc/ssl/certs \
    && chown -R trademaster:trademaster /app \
    && chown -R trademaster:trademaster /var/log/notification-service \
    && chown -R trademaster:trademaster /tmp/notification-service

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy SSL certificates (if available)
COPY --chown=trademaster:trademaster ssl-certs/* /etc/ssl/certs/ 2>/dev/null || true

# Set security configurations
RUN chown trademaster:trademaster app.jar \
    && chmod 644 app.jar

# Create health check script
RUN echo '#!/bin/bash\n\
curl -k --fail --silent --show-error \
  --max-time 10 \
  "https://localhost:8084/ops/health" \
  || curl --fail --silent --show-error \
     --max-time 10 \
     "http://localhost:8084/ops/health" \
  || exit 1' > /app/health-check.sh \
  && chmod +x /app/health-check.sh \
  && chown trademaster:trademaster /app/health-check.sh

# Switch to non-root user
USER trademaster

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseZGC -XX:+UseStringDeduplication"
ENV SPRING_THREADS_VIRTUAL_ENABLED=true
ENV SSL_ENABLED=true
ENV MANAGEMENT_PORT=8084

# Expose ports
EXPOSE 8084

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD ["/app/health-check.sh"]

# Set up JVM arguments for Java 24 Virtual Threads
ENV JVM_ARGS="\
    --enable-preview \
    -Dspring.threads.virtual.enabled=true \
    -XX:+UseZGC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Dlogging.config=classpath:logback-spring.xml"

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Start the application with optimized JVM settings
CMD ["sh", "-c", "exec java ${JVM_ARGS} ${JAVA_OPTS} -jar app.jar"]

# Labels for container metadata
LABEL maintainer="TradeMaster DevOps <devops@trademaster.com>"
LABEL version="2.0.0"
LABEL description="TradeMaster Notification Service - Enterprise Grade"
LABEL service="notification-service"
LABEL component="notification"
LABEL framework="spring-boot-3.5"
LABEL java-version="24"
LABEL virtual-threads="enabled"
LABEL security="zero-trust"
LABEL monitoring="prometheus"
LABEL logging="structured-json"