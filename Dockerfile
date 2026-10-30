# -------------------------------
# Stage 1: Build
# -------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration first so the dependency layer is cached
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build the executable JAR
COPY src ./src
RUN mvn -B package -DskipTests


# -------------------------------
# Stage 2: Run
# -------------------------------
FROM eclipse-temurin:21-jre AS run

WORKDIR /app

# Create the non-root user before copying, so the jar is owned by it
RUN useradd -m appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]