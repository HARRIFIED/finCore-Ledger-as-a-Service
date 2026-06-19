# Copyright (c) 2026-present Harrified tech and contributors
# SPDX-License-Identifier: AGPL-3.0-only
# See the LICENSE file for details.

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY account-service account-service
RUN chmod +x ./gradlew && ./gradlew account-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /workspace/account-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
