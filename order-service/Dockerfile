FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
ENV MAVEN_OPTS="-XX:+UseSerialGC -Xmx512m"
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]