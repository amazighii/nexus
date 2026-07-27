FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
WORKDIR /workspace/media-service
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/media-service/target/*.jar app.jar
ENV MAVEN_OPTS="-XX:+UseSerialGC -Xmx512m"
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
