# Copyright (c) 2026-present Harrified tech and contributors
# SPDX-License-Identifier: AGPL-3.0-only
# See the LICENSE file for details.

FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY account-service account-service
RUN chmod +x ./gradlew && ./gradlew account-service:bootJar -x test --no-daemon

# Distroless: no shell, no package manager, runs as uid 65532 (nonroot) by default.
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=builder /workspace/account-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
