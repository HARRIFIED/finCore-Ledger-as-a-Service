# Copyright (c) 2026-present Harrified tech and contributors
# SPDX-License-Identifier: AGPL-3.0-only
# See the LICENSE file for details.

FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /workspace

# Gradle wrapper and root build files
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

# Modules needed for this service (api-contracts generates the gRPC stubs)
COPY api-contracts api-contracts
COPY iam-service iam-service

RUN chmod +x ./gradlew && ./gradlew iam-service:bootJar -x test --no-daemon

# ── Runtime image ─────────────────────────────────────────────────────────────
# Distroless: no shell, no package manager, runs as uid 65532 (nonroot) by default.
# Far smaller CVE surface than eclipse-temurin:21-jre-alpine.
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=builder /workspace/iam-service/build/libs/*.jar app.jar

# HTTP and gRPC ports
EXPOSE 8092 9082

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
