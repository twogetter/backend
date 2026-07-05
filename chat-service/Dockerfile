# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x ./gradlew && ./gradlew :chat-service:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
RUN useradd -m spring
USER spring
WORKDIR /app
COPY --from=build /workspace/chat-service/build/libs/*.jar app.jar
EXPOSE 8600
ENTRYPOINT ["java", "-jar", "/app/app.jar"]