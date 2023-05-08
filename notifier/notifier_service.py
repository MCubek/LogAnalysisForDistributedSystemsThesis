from kafka import KafkaConsumer
import json
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError
import smtplib
from email.mime.text import MIMEText
from pushbullet import Pushbullet
import os

# Get environment variables
BOOTSTRAP_SERVER = os.getenv("BOOTSTRAP_SERVER", "localhost:9092")
SLACK_BOT_TOKEN = os.getenv("SLACK_BOT_TOKEN")
SLACK_CHANNEL = os.getenv("SLACK_CHANNEL")
PUSHBULLET_API_KEY = os.getenv("PUSHBULLET_API_KEY")
EMAIL_USERNAME = os.getenv("EMAIL_USERNAME")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD")
TOPIC_PATTERN = os.getenv("TOPIC_PATTERN", "notification-.*")

# Get email recipients from the config file
with open("/config/recipients", "r") as f:
    EMAIL_RECIPIENTS = f.read().strip().split(",")

# Initialize Slack client
slack_client = WebClient(token=SLACK_BOT_TOKEN)

# Initialize Pushbullet client
pb = Pushbullet(PUSHBULLET_API_KEY)

# Create Kafka consumer
consumer = KafkaConsumer(
    bootstrap_servers=BOOTSTRAP_SERVER,
    value_deserializer=lambda m: json.loads(m.decode("utf-8")),
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


# Function to send a push notification via Pushbullet
def send_pushbullet(message):
    try:
        pb.push_note("Kafka Notification", message)
    except Exception as e:
        print(f"Error sending Pushbullet notification: {e}")


# Process Kafka messages
for message in consumer:
    msg = message.value

    send_to_slack(msg)
    send_email(msg)
    send_pushbullet(msg)
