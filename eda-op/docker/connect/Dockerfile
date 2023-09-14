#Dockerfile for creating the custom connector image based on strimzi kafka

ARG ARCH=amd64
ARG STRIMZI_VERSION=0.33.0-kafka-3.3.2
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}-${ARCH}

ARG DEBEZIUM_MYSQL_CONNECTOR=debezium-connector-mysql

ENV KAFKA_CONNECT_PLUGIN_PATH=/opt/connectors

COPY ./plugins/${DEBEZIUM_MYSQL_CONNECTOR} $KAFKA_CONNECT_PLUGIN_PATH/${DEBEZIUM_MYSQL_CONNECTOR}/
