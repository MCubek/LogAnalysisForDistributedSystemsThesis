package hr.fer.stream.error_aggregator_stream.serde;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * @author matejc
 * Created on 09.05.2023.
 */

public class CustomJsonSerde implements Serde<JsonNode> {
    private final Serializer<JsonNode> jsonSerializer = new CustomJsonSerializer();
    private final Deserializer<JsonNode> jsonDeserializer = new CustomJsonDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        jsonSerializer.configure(configs, isKey);
        jsonDeserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        jsonSerializer.close();
        jsonDeserializer.close();
    }

    @Override
    public Serializer<JsonNode> serializer() {
        return jsonSerializer;
    }

    @Override
    public Deserializer<JsonNode> deserializer() {
        return jsonDeserializer;
    }
}
