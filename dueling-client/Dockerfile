# Use an appropriate base image with Java 21
FROM openjdk:21-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the client jar from the target directory to the container
COPY target/dueling-client-1.0-SNAPSHOT.jar app.jar

# Command to run the client application
# The main class is client.GameClient
ENTRYPOINT ["java", "-cp", "app.jar", "client.GameClient"]