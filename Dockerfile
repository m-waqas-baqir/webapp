# syntax=docker/dockerfile:1
# =============================================================================
# Real Investments API — Render Web Service
# =============================================================================
# Monorepo: Spring Boot lives in ./backend
#
# Render dashboard (Web Service → webapp-yt4i.onrender.com):
#   Runtime ............... Docker
#   Dockerfile path ....... ./Dockerfile
#   Docker context ........ .
#   Health check path ..... /actuator/health
#
# URLs:
#   Backend (this service)  https://webapp-yt4i.onrender.com
#   Frontend (static site)  https://webapp-fe.onrender.com
#
# Required env (set in Render → Environment; mark secrets as Secret):
#   SPRING_DATASOURCE_USERNAME .... realinvestments_user
#   SPRING_DATASOURCE_PASSWORD .... <Postgres password>
#   APP_SECURITY_JWT_SECRET ........ min 32 characters
#
# Database — cross-region (Web Service ≠ Postgres region):
#   DB_HOST ....................... dpg-d8dci43bc2fs73eg6ss0-a.singapore-postgres.render.com
#   DB_PORT ....................... 5432
#   DB_NAME ....................... realinvestments
#   DB_SSLMODE .................... require
#
# Database — same region only (internal hostname):
#   DB_HOST ....................... dpg-d8dci43bc2fs73eg6ss0-a
#   DB_SSLMODE .................... disable
#
# Optional (defaults baked into image below):
#   APP_CORS_ALLOWED_ORIGINS ....... https://webapp-fe.onrender.com,http://localhost:4200
#   APP_SEED_ENABLED ............... false
#   APP_STORAGE_ROOT ............... /app/data/uploads
# =============================================================================

# ---- Build -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests

COPY backend/src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

LABEL org.opencontainers.image.title="real-investments-api"
LABEL org.opencontainers.image.description="Spring Boot API for Render"

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/data/uploads \
    && chown -R app:app /app

USER app

COPY --from=build --chown=app:app /build/target/spring-backend-starter-*.jar /app/app.jar

# Render injects PORT at runtime
ENV PORT=8080

# Spring / app defaults (override in Render dashboard as needed)
ENV SPRING_PROFILES_ACTIVE=postgres
ENV APP_SEED_ENABLED=false
ENV APP_STORAGE_ROOT=/app/data/uploads
ENV APP_CORS_ALLOWED_ORIGINS=https://webapp-fe.onrender.com,http://localhost:4200
ENV DB_SSLMODE=require
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Dserver.port=${PORT} -jar /app/app.jar"]
