FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon bootJar
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S shop && adduser -S shop -G shop
COPY --from=build /app/build/libs/shop-billing-1.0.0.jar app.jar
USER shop
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
