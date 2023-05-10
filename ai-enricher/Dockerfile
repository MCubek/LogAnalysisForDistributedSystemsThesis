# Use the official Python image as the base image
FROM python:3.10

# Set the working directory
WORKDIR /app

# Copy the requirements file
COPY requirements.txt /app/requirements.txt

# Install the required packages
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application code
COPY ai-enricher.py /app/ai-enricher.py

# Run the application
CMD ["python", "ai-enricher.py"]
