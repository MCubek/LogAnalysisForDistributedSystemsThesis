#!/bin/bash

public_ip=$(curl -s https://api.ipify.org)

sed "s/MY_PUBLIC_IP/$public_ip/" kafka/kafka-cluster-template.yaml >kafka-cluster-temp.yaml

kubectl apply -f kafka-cluster-temp.yaml

rm kafka-cluster-temp.yaml
