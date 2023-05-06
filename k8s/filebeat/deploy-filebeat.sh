#!/bin/bash

public_ip=$(curl -s https://api.ipify.org)

sed "s/MY_PUBLIC_IP/$public_ip/" filebeat/filebeat-configMap-kafka-template.yaml >filebeat-config-map-temp.yaml

kubectl apply -f filebeat-config-map-temp.yaml -f filebeat/filebeat-kafka.yaml

rm filebeat-config-map-temp.yaml
