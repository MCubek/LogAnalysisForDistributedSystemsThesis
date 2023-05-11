import requests
from kafka import KafkaConsumer
import json
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError
import smtplib
from email.mime.text import MIMEText
import os

# Get environment variables
BOOTSTRAP_SERVER = os.getenv("BOOTSTRAP_SERVER", "localhost:9095")
SLACK_BOT_TOKEN = os.getenv("SLACK_BOT_TOKEN")
SLACK_CHANNEL = os.getenv("SLACK_CHANNEL")
EMAIL_USERNAME = os.getenv("EMAIL_USERNAME")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD")
TOPIC_PATTERN = os.getenv("TOPIC_PATTERN", "notification-.*")
PUSHOVER_API_KEY = os.getenv("PUSHOVER_API_KEY")
PUSHOVER_USER_KEY = os.getenv("PUSHOVER_USER_KEY")
EMAIL_USERS_PATH = os.getenv("EMAIL_USERS_PATH", "/config/recipients")

# Pushover API URL
PUSHOVER_API_URL = "https://api.pushover.net/1/messages.json"

# Get email recipients from the config file
with open(EMAIL_USERS_PATH, "r") as f:
    EMAIL_RECIPIENTS = f.read().strip().split(",")

# Initialize Slack client
slack_client = WebClient(token=SLACK_BOT_TOKEN)

# Create Kafka consumer
consumer = KafkaConsumer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    key_deserializer=lambda m: json.loads(m.decode("utf-8")),
    group_id="notification_group",
)

# Subscribe to topics with the given prefix pattern
consumer.subscribe(pattern=TOPIC_PATTERN)


# Function to send a message to Slack
def send_to_slack(message):
    try:
        _response = slack_client.chat_postMessage(
            channel=SLACK_CHANNEL,
            text=message
        )
    except SlackApiError as e:
        print(f"Error sending message to Slack: {e}")


# Function to send an email
def send_email(message):
    msg = MIMEText(message)
    msg["Subject"] = "Kafka Notification"
    msg["From"] = EMAIL_USERNAME

    for recipient in EMAIL_RECIPIENTS:
        msg["To"] = recipient
        try:
            with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
                server.login(EMAIL_USERNAME, EMAIL_PASSWORD)
                server.sendmail(EMAIL_USERNAME, recipient, msg.as_string())
        except Exception as e:
            print(f"Error sending email: {e}")


# Function to send a push notification via Pushover
def send_pushover(message):
    data = {
        "token": PUSHOVER_API_KEY,
        "user": PUSHOVER_USER_KEY,
        "message": message,
        "title": "Kafka Notification"
    }
    try:
        response = requests.post(PUSHOVER_API_URL, data=data)
        response.raise_for_status()
    except Exception as e:
        print(f"Error sending Pushover notification: {e}")


# Process Kafka messages
def extract_message(message):
    message_key = message.key
    message_value = message.value

    output = f'Service {message_key["service"]}\nK8s: namespace={message_key["kubernetes"]["namespace"]}, pod={message_key["kubernetes"]["pod"]}\nMessage: {message_key["message"]}\nCount: {message_value["count"]}\nFirst Timestamp: {message_value["firstTimestamp"]}'

    if "instructions" in message_value:
        output += f'\nInstructions: {message_value["instructions"]}'

    return output


for message in consumer:
    msg = extract_message(message)

    send_to_slack(msg)
    send_email(msg)
    send_pushover(msg)
