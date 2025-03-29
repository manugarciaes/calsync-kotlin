FROM gradle:7.6.1-jdk17 as builder

WORKDIR /app

# Copy build configuration files
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src/ src/

# Build the application
RUN gradle build --no-daemon

# Runtime stage
FROM openjdk:17-slim

WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set environment variables
ENV DB_URL=jdbc:postgresql://postgres:5432/calsync
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=postgres
ENV MAIL_HOST=smtp.gmail.com
ENV MAIL_PORT=587
ENV CALENDAR_SYNC_INTERVAL_MINUTES=15

# Expose application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]