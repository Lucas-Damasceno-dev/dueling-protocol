#!/bin/bash
echo ">>> Limpando e construindo o projeto com Maven..."
mvn clean package
echo ">>> Build concluído! O JAR executável está em target/dueling-protocol-1.0-SNAPSHOT.jar"
