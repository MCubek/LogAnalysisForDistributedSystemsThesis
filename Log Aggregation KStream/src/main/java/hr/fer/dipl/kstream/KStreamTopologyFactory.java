package hr.fer.dipl.kstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hr.fer.dipl.kstream.serde.SerdeFactory.jsonSerde;

/**
 * @author matejc
 * Created on 05.05.2023.
 */

public class KStreamTopologyFactory {

    private KStreamTopologyFactory() {
    }

    static StreamsBuilder createTopology(Properties config, String inputTopicPattern, String outputTopic) {
        StreamsBuilder builder = new StreamsBuilder();
        Pattern pattern = Pattern.compile(inputTopicPattern);

        // List and filter topics
        Set<String> inputTopics = listAndFilterTopics(config, pattern);


        // Process each input topic
        for (String inputTopic : inputTopics) {
            KStream<String, JsonNode> source = builder.stream(inputTopic);

            // Filter messages with "ERROR" level
//            KStream<String, JsonNode> errorMessages = source.filter((key, value) -> "ERROR".equals(value.get("level").asText()));
            var errorMessages = source;

            KStream<String, JsonNode> aggregated = errorMessages
                    .groupBy((key, value) -> value.get("message").asText())
                    .aggregate(
                            JsonNodeFactory.instance::arrayNode,
                            (aggKey, newVal, aggVal) -> {
                                ((ArrayNode) aggVal).add(newVal);
                                return aggVal;
                            },
                            Materialized.<String, JsonNode, KeyValueStore<Bytes, byte[]>>as("store-" + inputTopic)
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(jsonSerde())
                    )
                    .toStream();

            // Include the original topic name in the output message
            KStream<String, JsonNode> withTopicName = aggregated.mapValues((key, value) -> {
                ((ObjectNode) value).put("origin_topic", inputTopic);
                return value;
            });

            withTopicName.to(outputTopic);
        }

        return builder;
    }

    private static Set<String> listAndFilterTopics(Properties config, Pattern pattern) {
        try (AdminClient adminClient = AdminClient.create(config)) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            KafkaFuture<Set<String>> namesFuture = listTopicsResult.names();
            Set<String> allTopicNames = namesFuture.get();
            return allTopicNames.stream()
                    .filter(topicName -> pattern.matcher(topicName).matches())
                    .collect(Collectors.toSet());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error listing and filtering topics", e);
        }
    }
}
