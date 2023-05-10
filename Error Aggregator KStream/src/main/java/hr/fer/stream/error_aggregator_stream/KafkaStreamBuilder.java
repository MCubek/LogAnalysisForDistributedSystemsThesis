package hr.fer.stream.error_aggregator_stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hr.fer.stream.error_aggregator_stream.exception.KStreamAggregationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.SlidingWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author matejc
 * Created on 08.05.2023.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaStreamBuilder {

    @Value("${input.topic.pattern}")
    private String inputTopicPattern;

    @Value("${output.topic}")
    private String outputTopic;

    private final KafkaStreamsConfiguration kafkaStreamsConfiguration;

    @Autowired
    public void buildPipeline(StreamsBuilder builder) {
        Pattern pattern = Pattern.compile(inputTopicPattern);

        Set<String> inputTopics = listAndFilterTopics(pattern);

        for (String inputTopic : inputTopics) {
            KStream<JsonNode, JsonNode> source = builder.stream(inputTopic);

            // Filter messages with "ERROR" level
            KStream<JsonNode, JsonNode> errorMessages = source.filter(KafkaStreamBuilder::isValidErrorMessage);

            KStream<Windowed<JsonNode>, JsonNode> aggregated = errorMessages
                    .groupByKey()
                    .windowedBy(SlidingWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(30)))
                    .aggregate(() -> (JsonNode) new ArrayNode(JsonNodeFactory.instance), ((key, value, aggregate) -> {
                        if (aggregate.isArray()) {
                            ((ArrayNode) aggregate).add(value);
                            return aggregate;
                        } else {
                            return JsonNodeFactory.instance.arrayNode().add(value);
                        }
                    }))
                    .toStream();

            KStream<JsonNode, JsonNode> mapped = aggregated
                    .map((k, v) -> {
                        int count = v.size();

                        ObjectNode result = JsonNodeFactory.instance.objectNode();
                        result.put("count", count);
                        result.set("messages", v);
                        result.set("firstTimestamp", v.get(0).get("@timestamp"));

                        return new KeyValue<>(k.key(), result);

                    });

            mapped.to(outputTopic);
        }
    }

    private static boolean isValidErrorMessage(JsonNode key, JsonNode value) {
        if (key == null || key.isEmpty()) return false;

        if (value != null && value.has("level")) {
            return "ERROR".equals(value.get("level").asText());
        }

        return false;
    }

    private Set<String> listAndFilterTopics(Pattern pattern) {
        try (AdminClient adminClient = AdminClient.create(kafkaStreamsConfiguration.asProperties())) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            KafkaFuture<Set<String>> namesFuture = listTopicsResult.names();
            Set<String> allTopicNames = namesFuture.get();
            return allTopicNames.stream()
                    .filter(topicName -> pattern.matcher(topicName).matches())
                    .collect(Collectors.toSet());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while listing and filtering topics.");
            throw new KStreamAggregationException(e);
        }
    }
}
