# ────────────────────────────────────────────────────────────────
# Stage 1 – Build
#   Base: official Maven image with Eclipse Temurin JDK 21
#   Goal: compile sources and produce the fat JAR
# ────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy dependency manifest first so Docker can cache the layer
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy sources and build, skipping tests (tests run in CI, not in image build)
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ────────────────────────────────────────────────────────────────
# Stage 2 – Runtime
#   Base: Eclipse Temurin JRE 21 on Alpine (minimal footprint)
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Upload directory — must be writable by the app user
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads

# Copy fat JAR from builder stage
COPY --from=builder /build/target/evidence-pilot-be-*.jar app.jar

USER appuser

EXPOSE 8080

# Use exec form so signals are forwarded properly to the JVM
ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]
