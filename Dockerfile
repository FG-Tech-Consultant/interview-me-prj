# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci --only=production

COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend
FROM gradle:8.5-jdk21-alpine AS backend-build

WORKDIR /app

# Copy Gradle configuration
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Copy common module
COPY common ./common

# Copy backend module
COPY backend ./backend

# Copy frontend dist to backend static resources
COPY --from=frontend-build /app/frontend/dist ./backend/src/main/resources/static

# Build backend (skip frontend build since we already have dist)
RUN gradle :backend:bootJar --no-daemon -x test -x copyFrontend

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from backend build
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
