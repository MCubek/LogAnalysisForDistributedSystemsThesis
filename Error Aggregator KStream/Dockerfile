FROM openjdk:17
LABEL authors="matejc"

ENV PROFILE=local

WORKDIR /app
COPY build/libs/error_aggregator_stream-1.0.0.jar /app/log_aggregation_kstream.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar","-Dspring.profiles.active=${PROFILE}" ,"/app/log_aggregation_kstream.jar"]

USER 1002