package hr.fer.dipl.kstream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static hr.fer.dipl.kstream.serde.SerdeFactory.jsonSerde;

/**
 * @author matejc
 * Created on 05.05.2023.
 */
public class LogsAggregatorRunner {

    private static final Logger logger = LoggerFactory.getLogger(LogsAggregatorRunner.class);

    public static void main(String[] args) {

        logger.info("Started");

        Properties config = new Properties();

        // Get arguments as from env vars
        String kafkaBootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (kafkaBootstrapServers == null || kafkaBootstrapServers.isBlank()) {
            throw new IllegalArgumentException("KAFKA_BOOTSTRAP_SERVERS environment variable not set");
        }
        String inputTopicPattern = System.getenv("INPUT_TOPIC_PATTERN");
        if (inputTopicPattern == null || inputTopicPattern.isEmpty()) {
            throw new IllegalArgumentException("INPUT_TOPIC_PATTERN environment variable not set");
        }
        String outputTopic = System.getenv("OUTPUT_TOPIC");
        if (outputTopic == null || outputTopic.isEmpty()) {
            throw new IllegalArgumentException("OUTPUT_TOPIC environment variable not set");
        }


        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);

        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-aggregator");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        //noinspection resource
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, jsonSerde().getClass().getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = KStreamTopologyFactory.createTopology(config, inputTopicPattern, outputTopic);

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}