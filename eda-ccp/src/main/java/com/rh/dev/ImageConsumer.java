package com.rh.dev;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.logging.Log;

@ApplicationScoped
public class ImageConsumer {

    @Inject
    Configuration config;

    public void consumeRecords() {
        
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.appKafkaBootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,config.appKafkaRecordKeySerde);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,config.appKafkaConsumerGroupId.orElse(UUID.randomUUID().toString()));
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        Serde<Bytes> s3backedSerde = SerdeUtil.createBackingSerdeS3(config, Bytes.class, Serdes.BytesSerde.class, false);
        
        try(var consumer = new KafkaConsumer<>(
                            properties,
                            Serdes.String().deserializer(),
                            s3backedSerde.deserializer())) {

            Log.info("start to consume records from topic "+config.appKafkaTopic+"...");
            consumer.subscribe(List.of(config.appKafkaTopic));
            while (true) {
                var records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Bytes> record : records) {
                    Log.info("consumed image with key "+record.key()+ " -> 'execute business logic...'");
                    byte[] rawData = record.value().get();
                    var image = ImageIO.read(new ByteArrayInputStream(rawData));
                    Log.info("IMAGE META DATA -> width: "+image.getWidth()+ " | height: "+image.getHeight() + " | size: "+rawData.length);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

}
