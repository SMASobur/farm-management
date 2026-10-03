# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT env var
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]