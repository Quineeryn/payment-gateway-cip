# --- Build Stage ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Run Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add a non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Application Configuration
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080

# Labels for documentation
LABEL version="0.0.1-SNAPSHOT"
LABEL description="Payment Gateway"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
