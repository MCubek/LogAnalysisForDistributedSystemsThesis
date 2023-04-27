# K8s Setup

- ## Minikube setup

    1. Start minikube node and control plane

        ```bash
        minikube start --memory=10240 --kubernetes-version=v1.26.3 --ports=30096:30096
        ```

    2. Start minikube tunnel

        ```bash
        minikube tunnel
        ```

    <!-- 3. Start minikube registry

        ```bash
        minikube addons enable registry
        ``` -->

- ## Strimzi setup

    1. Create k8s kafka namespace

        ```bash
        kubectl create ns kafka
        ```

    2. Install Strimzi operators

        ```bash
        kubectl apply -f ./strimzi-0.34.0/install/cluster-operator -n kafka
        ```

    3. Start kafka cluster

        ```bash
        kubectl apply -f kafka/kafka-cluster.yaml
        ```

    4. Wait for kafka cluster

        ```bash
        kubectl wait --for=condition=ready pod -l strimzi.io/name=masters-cluster-kafka -n kafka
        ```

    5. Start supporting kafka services

        ```bash
        kubectl apply -f kafka/schema-registry.yaml -f kafka/kafka-ui.yaml
        kubectl patch svc masters-cluster-kafka-externaltls-bootstrap -n kafka --type='json' -p='[{"op": "replace", "path": "/spec/ports/0/nodePort", "value":30096}]'
        ```

    6. Create kafka user credentials

        ```bash
        kubectl apply -f kafka/kafkauser-matej.yaml
        ```

    7. Export kafka user keystore and print keystore password

        ```bash
        kubectl get secrets -n kafka kafkauser-matej -o jsonpath='{.data.user\.p12}' | base64 -d > kafka/keystore.p12
        kubectl get secrets -n kafka kafkauser-matej -o jsonpath='{.data.user\.password}' | base64 -d > kafka/keystore.password
        ```

    8. Export kafka broker ca and create a jks and pem

        ```bash
        kubectl get secret masters-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 --decode > kafka/ca.p12
        kubectl get secret masters-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 --decode > kafka/ca.password

        keytool -importkeystore -srckeystore kafka/ca.p12 -srcstoretype PKCS12 -srcstorepass $(cat kafka/ca.password) -destkeystore kafka/ca.jks -deststoretype JKS -deststorepass kafkapass

        openssl pkcs12 -in kafka/ca.p12 -out kafka/ca.pem -nokeys -cacerts -passin pass:$(cat kafka/ca.password)

        openssl pkcs12 -in kafka/keystore.p12 -nokeys -out kafka/certificate.pem -passin pass:$(cat kafka/keystore.password)
        openssl pkcs12 -in kafka/keystore.p12 -nocerts -nodes -out kafka/key.pem -passin pass:$(cat kafka/keystore.password)
        ```

    9. Replace password in ssl-client.properties with keystore.password value

- ## Elastic setup

    1. Create Elastic CRDs

        ```bash
        kubectl create -f https://download.elastic.co/downloads/eck/2.7.0/crds.yaml
        ```

    2. Create Elastic operator

        ```bash
        kubectl apply -f https://download.elastic.co/downloads/eck/2.7.0/operator.yaml
        ```

    3. Create elk namespace

        ```bash
        kubectl create ns elk
        ```

    4. Create Elastic cluster

        ```bash
        kubectl apply -f elk/elk-cluster.yaml
        ```

    5. Get generated password for *elastic* user

        ```bash
        kubectl get secret -n elk masters-es-elastic-user -o go-template='{{.data.elastic | base64decode}}'
        ```

    6. Create Kibana

        ```bash
        kubectl apply -f elk/kibana.yaml
        ```

    7. Login into Kibana with password from step 5 and username *elastic*

- ## Grafana setup

    1. Create Grafana namespace

        ```bash
        kubectl create ns grafana
        ```

    2. Create Grafana cluster

        ```bash
        kubectl apply -f elk/grafana.yaml
        ```

- ## Kafka credentials on cloudvane cluster and deploy filebeat

    1. Create secrets

        ```bash
        k create secret generic kafka-matej-credentials --from-file=kafka/ca.jks --from-file=kafka/keystore.p12 --from-file=kafka/keystore.password --from-file=kafka/ssl-client.properties -n kafka

        kubectl create secret generic kafka-matej-ssl-secret \
        --from-file=ca.pem=./kafka/ca.pem \
        --from-file=certificate.pem=./kafka/certificate.pem \
        --from-file=key.pem=./kafka/key.pem \
        -n elk
        ```

    2. Deploy Filebeat daemonset

        ```bash
        kubectl apply -f kafka/filebeat/filebeat-kafka.yaml
        ```

    3. ```bash
        /opt/kafka/bin/kafka-topics.sh --bootstrap-server vrbanizagreb.ddns.net:30096 --command-config ssl-client.properties --list
        ```
