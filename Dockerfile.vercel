FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json ./
RUN npm install --ignore-scripts
COPY frontend/ ./
RUN npm run build
FROM gradle:8.14.3-jdk17 AS backend
WORKDIR /app
COPY backend/ ./
COPY --from=frontend /app/dist/ src/main/resources/static/
RUN gradle --no-daemon bootJar
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S shop && adduser -S shop -G shop
COPY --from=backend /app/build/libs/shop-billing-1.0.0.jar app.jar
USER shop
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
