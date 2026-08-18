# ---- Build stage ----
FROM gradle:8.9-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S quickchat && adduser -S quickchat -G quickchat
COPY --from=build /app/build/libs/*.jar app.jar
USER quickchat
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx768m", "-jar", "app.jar"]
