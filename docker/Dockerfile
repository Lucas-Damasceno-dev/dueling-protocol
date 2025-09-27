# Stage 1: Build - Uses an image with Maven and JDK 21 to compile the project
# UPDATED: Using an Eclipse Temurin image with OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copies Maven configuration files and downloads dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copies source code and compiles, generating the .jar
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run - Uses a lightweight image with only Java Runtime 21 to run the application
# UPDATED: Using a slim JRE (Java Runtime Environment) image with OpenJDK 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copies the .jar generated in the build stage to the final image
COPY --from=build /app/target/dueling-protocol-1.0-SNAPSHOT.jar app.jar

# Exposes the ports the server will use (TCP for the game, UDP for ping)
EXPOSE 7777/tcp
EXPOSE 7778/udp

# Command to start the server when the container is executed
CMD ["java", "-jar", "app.jar"]
