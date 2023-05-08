FROM openjdk:17
LABEL authors="matejc"

WORKDIR /app
COPY target/log_aggregation_kstream-1.0-shaded.jar /app/log_aggregation_kstream.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/log_aggregation_kstream.jar"]