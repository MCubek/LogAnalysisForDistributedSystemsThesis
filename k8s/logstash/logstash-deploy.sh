#!/bin/bash

elastic_pass=$(kubectl get secret -n elk masters-es-elastic-user -o go-template='{{.data.elastic | base64decode}}')

sed "s/ELASTIC_PASSWORD/$elastic_pass/" logstash/logstash-config-pipelines-template.yaml >logstash-config-pipelines.yaml

kubectl apply -f logstash/logstash-config.yaml -f logstash-config-pipelines.yaml -f logstash/logstash-src.yaml

rm logstash-config-pipelines.yaml
