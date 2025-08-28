#!/bin/bash

# Clean previous builds
rm -rf target
mkdir -p target/classes

# Compile all Java files
javac -d target/classes \\
  src/main/java/model/*.java \\
  src/main/java/repository/*.java \\
  src/main/java/service/matchmaking/*.java \\
  src/main/java/service/store/*.java \\
  src/main/java/controller/*.java \\
  src/main/java/*.java

echo "Compilation completed successfully!"