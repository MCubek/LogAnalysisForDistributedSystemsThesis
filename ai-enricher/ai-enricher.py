import json
import os
from datetime import datetime, timedelta

import requests
from kafka import KafkaConsumer, KafkaProducer

# Get environment variables
BOOTSTRAP_SERVER = os.getenv("BOOTSTRAP_SERVER", "localhost:9095")
TOPIC_REGEX = os.getenv("TOPIC_REGEX", "aggregated-errors.*")
OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "enriched-errors")
RATE_LIMIT = int(os.getenv("RATE_LIMIT", 120))
AI_MODEL = os.getenv("AI_MODEL", "text-davinci-003")

# Initialize Kafka consumer and producer
consumer = KafkaConsumer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    group_id="enrichment_group",
    auto_offset_reset="earliest",
)

producer = KafkaProducer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_serializer=lambda m: json.dumps(m).encode("utf-8"),
    compression_type="snappy"
)

# Subscribe to topics with the given regex pattern
consumer.subscribe(pattern=TOPIC_REGEX)

# Set up rate limiting
interval = timedelta(hours=1)
allowed_requests = RATE_LIMIT
request_count = 0
time_window_start = datetime.now()


# Function to call ChatGPT API for enhancement
def enhance_message(kafka_message):
    global request_count, time_window_start

    json_value = kafka_message.value

    # Ger first error out of aggregation
    error = json_value.get("messages")[0]
    error["count"] = json_value.get("count")

    # Extract the 'message' and 'stacktrace' fields from the JSON value
    message = error.get("message", "")
    stacktrace = error.get("stacktrace", "")

    if not message:
        print("No 'message' field found in the Kafka message value. Skipping enhancement.")
        return error

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
        "temperature": 0.6,
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
    print(f'Received message {msg.value}\n')
    enhanced_message = enhance_message(msg)
    print('Enhanced message.\n')
    producer.send(OUTPUT_TOPIC, key=msg.key, value=enhanced_message)
    print(f'Sent response to topic {OUTPUT_TOPIC}.\n')
