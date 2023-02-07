package com.rh.dev;

import java.util.Optional;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class Configuration {

    @ConfigProperty(name="app.num.records", defaultValue="10")
    int appNumRecords;

    @ConfigProperty(name="app.delay.millis", defaultValue="1000")
    long appDelayMs;
    
    @ConfigProperty(name="app.path.to.images")
    String appPathToImages;

    @ConfigProperty(name="app.kafka.bootstrap.servers")
    String appKafkaBootstrapServers;
    
    @ConfigProperty(name="app.kafka.topic")
    String appKafkaTopic;

    @ConfigProperty(name="app.kafka.record.key.serde")
    String appKafkaRecordKeySerde;

    @ConfigProperty(name="app.kafka.record.value.serde")
    String appKafkaRecordValueSerde;

    @ConfigProperty(name="app.kafka.consumer.group.id")
    Optional<String> appKafkaConsumerGroupId;

    @ConfigProperty(name="serde.s3.base.path")
    String serdeS3BasePath;

    @ConfigProperty(name="serde.s3.endpoint")
    String serdeS3EndPoint;
    
    @ConfigProperty(name="serde.s3.region")
    String serdeS3Region;
    
    @ConfigProperty(name="serde.s3.access.key")
    String serdeS3AccessKey;
    
    @ConfigProperty(name="serde.s3.secret.key")
    String serdeS3SecretKey;
    
    @ConfigProperty(name="serde.s3.with.headers")
    boolean serdeS3WithHeaders;

    @ConfigProperty(name="serde.s3.max.bytes.size")
    int serdeS3MaxBytesSize;

}
