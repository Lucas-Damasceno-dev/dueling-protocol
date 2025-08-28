#!/bin/bash

# Clean previous builds
rm -rf project/target
mkdir -p project/target/classes

# Compile all Java files with correct classpath
javac -cp project/src/main/java -d project/target/classes \
  project/src/main/java/model/*.java \
  project/src/main/java/repository/*.java \
  project/src/main/java/service/matchmaking/*.java \
  project/src/main/java/service/store/*.java \
  project/src/main/java/controller/*.java \
  project/src/main/java/*.java

echo "Compilation completed successfully!"