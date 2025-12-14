# Multi-stage build for smaller final image
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# ===========================================
# Runtime Stage
# ===========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create data directories
RUN mkdir -p /app/data/FISIERE /app/data/backups /app/logs && \
    chown -R spring:spring /app

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run application
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
    "-jar", \
    "app.jar"]
