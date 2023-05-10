package hr.fer.stream.error_aggregator_stream.config;

import com.fasterxml.jackson.databind.JsonNode;
import hr.fer.stream.error_aggregator_stream.serde.CustomJsonSerde;
import hr.fer.stream.error_aggregator_stream.time.LogTimestampExtractor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.streams.StreamsConfig.*;

/**
 * @author matejc
 * Created on 08.05.2023.
 */

@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;
    @Value(value = "${spring.kafka.streams.application-id}")
    private String applicationId;
    @Value(value = "${spring.kafka.consumer.auto-offset-reset}")
    private String offset;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(APPLICATION_ID_CONFIG, applicationId);
        props.put("auto.offset.reset", offset);
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, CustomJsonSerde.class.getName());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, CustomJsonSerde.class.getName());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, LogTimestampExtractor.class.getName());

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<JsonNode> jsonSerde() {
        return new CustomJsonSerde();
    }

    @Bean
    public Serde<String> stringSerde() {
        return Serdes.String();
    }
}
