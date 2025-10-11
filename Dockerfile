# Use an appropriate base image with Java 21
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the executable fat jar from the target directory to the container
# The final name of the jar is determined by the pom.xml
COPY target/dueling-protocol-1.0-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
