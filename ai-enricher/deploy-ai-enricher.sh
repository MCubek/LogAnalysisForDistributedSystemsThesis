#!/bin/bash

# Exit on error
set -e

# Check if Docker is installed
if ! command -v docker &>/dev/null; then
  echo "Docker is not installed. Please install Docker and try again."
  exit 1
fi

# Get Docker Hub username and image version from environment variables
DOCKER_HUB_USERNAME="${DOCKER_HUB_USERNAME}"
IMAGE_VERSION="1.0"

if [[ -z "${DOCKER_HUB_USERNAME}" ]]; then
  echo "DOCKER_HUB_USERNAME environment variable not set. Please set it and try again."
  exit 1
fi

if [[ -z "${IMAGE_VERSION}" ]]; then
  echo "IMAGE_VERSION environment variable not set. Please set it and try again."
  exit 1
fi

# Set the name of your Docker image
IMAGE_NAME="kafka-ai-enricher"

# Build the Docker image
echo "Building the Docker image..."
docker build -t "${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${IMAGE_VERSION}" .

# Log in to Docker Hub
echo "Logging in to Docker Hub..."
docker login

# Push the Docker image to Docker Hub
echo "Pushing the Docker image to Docker Hub..."
docker push "${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${IMAGE_VERSION}"

echo "Deployment to Docker Hub completed successfully."
