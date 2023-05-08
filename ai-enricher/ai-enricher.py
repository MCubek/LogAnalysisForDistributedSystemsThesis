import json
import os
from datetime import datetime, timedelta

import requests
from kafka import KafkaConsumer, KafkaProducer

# Get environment variables
BOOTSTRAP_SERVER = os.getenv("BOOTSTRAP_SERVER", "localhost:9092")
TOPIC_REGEX = os.getenv("TOPIC_REGEX", "error-.*")
OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "enriched-messages")
RATE_LIMIT = int(os.getenv("RATE_LIMIT", 60))
AI_MODEL = os.getenv("AI_MODEL", "text-davinci-003")

# Initialize Kafka consumer and producer
consumer = KafkaConsumer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    group_id="enrichment_group",
)

producer = KafkaProducer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_serializer=lambda m: json.dumps(m).encode("utf-8"),
)

# Subscribe to topics with the given regex pattern
consumer.subscribe(pattern=TOPIC_REGEX)

# Set up rate limiting
interval = timedelta(hours=1)
allowed_requests = RATE_LIMIT
request_count = 0
time_window_start = datetime.now()


# Function to call ChatGPT API for enhancement
def enhance_message(json_value):
    global request_count, time_window_start

    # Extract the 'message' and 'stacktrace' fields from the JSON value
    message = json_value.get("message", "")
    stacktrace = json_value.get("stacktrace", "")

    if not message:
        print("No 'message' field found in the Kafka message value. Skipping enhancement.")
        return json_value

    # Check rate limit
    now = datetime.now()
    if now - time_window_start > interval:
        request_count = 0
        time_window_start = now

    if request_count >= allowed_requests:
        print("Rate limit exceeded. Skipping enhancement.")
        return json_value

    # Call ChatGPT API
    openai_secret = os.environ["OPENAI_API_KEY"]
    headers = {"Authorization": f"Bearer {openai_secret}"}
    url = "https://api.openai.com/v1/completions"

    prompt = f"How do I fix this error? {message}\n{stacktrace}"

    data = {
        "prompt": prompt,
        "model": AI_MODEL,
        "max_tokens": 300,
        "n": 1,
        "stop": None,
        "temperature": 1.0,
    }

    try:
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        result = response.json()
        enhanced_message = result["choices"][0]["text"].strip()
        request_count += 1

        # Add the result to the kafka message
        json_value["instructions"] = enhanced_message
        return json_value
    except Exception as e:
        print(f"Error in ChatGPT API call: {e}")
        return json_value


# Process Kafka messages
for msg in consumer:
    message = msg.value
    enhanced_message = enhance_message(message)
    producer.send(OUTPUT_TOPIC, enhanced_message)
