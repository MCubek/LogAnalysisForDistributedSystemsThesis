package hr.fer.stream.error_aggregator_stream.exception;

/**
 * @author matejc
 * Created on 10.05.2023.
 */

public class KStreamAggregationException extends RuntimeException {

    public KStreamAggregationException(Throwable cause) {
        super(cause);
    }

}
