#!/usr/bin/env bash
echo ">>> Building the project with Docker..."
docker build --no-cache -t dueling-protocol -f docker/Dockerfile .
echo ">>> Build completed! The Docker image is ready for use."
