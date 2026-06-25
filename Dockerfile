# Build stage — gradlew로 wrapper 명시 버전 사용 (재현성 보장)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseContainerSupport"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
