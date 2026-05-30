# Real Investments API — Render.com Web Service
# Build context: repository root (monorepo). Backend lives in ./backend
#
# Render: New → Web Service → connect repo → Runtime: Docker
# Required env (example):
#   SPRING_PROFILES_ACTIVE=postgres
#   SPRING_DATASOURCE_URL=jdbc:postgresql://...
#   SPRING_DATASOURCE_USERNAME=...
#   SPRING_DATASOURCE_PASSWORD=...
#   APP_SECURITY_JWT_SECRET=<min 32 chars>
#   APP_CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
#   APP_SEED_ENABLED=false

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data/uploads \
    && chown -R app:app /app

USER app

COPY --from=build /app/target/spring-backend-starter-*.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV APP_STORAGE_ROOT=/app/data/uploads

EXPOSE 8080

# Render injects PORT; Spring Boot reads server.port from this property.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
