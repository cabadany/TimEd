# Use Amazon Corretto 17 (a production-ready distribution of OpenJDK)
FROM amazoncorretto:17-alpine

# Set working directory
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create a non-root user for security
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring

# Change ownership of the app directory
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port (Render will use the PORT environment variable)
EXPOSE 8080

# Run the application with optimized JVM settings for containers
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "target/TimEd-0.0.1-SNAPSHOT.jar"] 