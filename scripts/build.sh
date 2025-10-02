#!/usr/bin/env bash
echo ">>> Building the project with Docker..."
docker build -t dueling-protocol -f docker/Dockerfile .
echo ">>> Build completed! The Docker image is ready for use."
