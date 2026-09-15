FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_API_URL=/api
ENV VITE_API_TIMEOUT_MS=15000
ENV VITE_TOAST_DURATION_MS=3000
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S btms && adduser -S btms -G btms
COPY --from=backend-build /workspace/backend/target/sms-0.0.1-SNAPSHOT.jar app.jar
USER btms
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
