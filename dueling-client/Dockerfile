# Stage 1: Build - Uses an image with Maven and JDK 21 to compile the client
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the entire project source
COPY . .

# Build the client JAR by running package on the specific module
RUN mvn package -DskipTests -pl dueling-client

# Stage 2: Run - Uses a lightweight image with only Java Runtime 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the client jar from the build stage's correct target directory
COPY --from=build /app/dueling-client/target/dueling-client-1.0-SNAPSHOT.jar app.jar

# Copy the startup script
COPY --from=build /app/dueling-client/start-client.sh start-client.sh
RUN chmod +x start-client.sh

# Command to run the client application
ENTRYPOINT ["/start-client.sh"]