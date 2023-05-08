## How to Deploy Your Docker Image to Docker Hub

Follow the steps below to deploy your Docker image to Docker Hub:

### Prerequisites

1. Ensure that you have [Maven](https://maven.apache.org/install.html) and [Docker](https://docs.docker.com/get-docker/)
   installed on your machine.
2. Replace `your_docker_hub_username` in the script with your actual Docker Hub username.

### Steps to Deploy

1. Open a terminal and navigate to the directory containing the `deploy-to-dockerhub.sh` script.

2. Make the script executable by running:

   ```bash
   chmod +x deploy-to-dockerhub.sh
   ```
   
3. Set the `DOCKER_HUB_USERNAME` and `IMAGE_VERSION` environment variables inline and run the script:

   ```bash
   DOCKER_HUB_USERNAME="your_docker_hub_username" IMAGE_VERSION="1.0" ./deploy-streams-image.sh
   ```
   Replace `your_docker_hub_username` with your actual DockerHub username, and adjust the `IMAGE_VERSION` value if
   needed.

4. The script will build the Maven project, build the Docker image, log in to Docker Hub, and push the image to your
   Docker Hub repository.

5. Once the script has completed successfully, your Docker image will be available on Docker Hub.