package hr.fer.stream.error_aggregator_stream.time;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.Instant;

/**
 * @author matejc
 * Created on 09.05.2023.
 */

public class LogTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        String timestampString = ((ObjectNode) record.value()).get("@timestamp").asText();
        Instant timestampInstant = Instant.parse(timestampString);

        return timestampInstant.toEpochMilli();
    }
}
