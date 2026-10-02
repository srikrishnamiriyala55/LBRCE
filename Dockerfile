FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_API_URL=/api
ENV VITE_API_TIMEOUT_MS=45000
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
RUN addgroup -S btms && adduser -S btms -G btms && mkdir -p /tmp/tomcat-sessions && chown -R btms:btms /app /tmp/tomcat-sessions
COPY --chown=btms:btms --from=backend-build /workspace/backend/target/sms-0.0.1-SNAPSHOT.jar app.jar
USER btms
EXPOSE 10000
ENV SESSION_STORE_DIR=/tmp/tomcat-sessions
# Render injects PORT at runtime. Bind explicitly on all interfaces so its port scanner can reach Tomcat.
ENTRYPOINT ["sh", "-c", "exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
