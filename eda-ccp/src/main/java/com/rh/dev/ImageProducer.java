package com.rh.dev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.logging.Log;

@ApplicationScoped
public class ImageProducer {

    @Inject
    Configuration config;

    private static Random RND = new Random();

    public void produceRecords() {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.appKafkaBootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.appKafkaRecordKeySerde);
        Serde<Bytes> s3backedSerde = SerdeUtil.createBackingSerdeS3(config,Bytes.class,Serdes.BytesSerde.class,false);
        
        try (var producer = new KafkaProducer<>(properties,
                                Serdes.String().serializer(),
                                s3backedSerde.serializer())) {
            
            var pathToImages = Path.of(config.appPathToImages);
            var fileEntries = Files.list(pathToImages).collect(Collectors.toList());

            Log.info("start to produce records to topic "+config.appKafkaTopic+"...");
            
            for (int i = 0; i < config.appNumRecords; i++) {
                var imagePath = fileEntries.get(RND.nextInt(0, fileEntries.size()));
                Log.info("producing data for image: "+ imagePath);
                var producerRecord = new ProducerRecord<String, Bytes>(
                        config.appKafkaTopic,
                        imagePath.toString(),
                        Bytes.wrap(Files.readAllBytes(imagePath))
                );
                producer.send(producerRecord, new Callback() {
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if (e == null) {
                            Log.info("meta data: " + "Topic:" + recordMetadata.topic() + " " +
                                        "Partition: " + recordMetadata.partition() + " " +
                                        "Offset: " + recordMetadata.offset() + " " +
                                        "Timestamp: " + recordMetadata.timestamp()
                            );
                        } else {
                            Log.error("error while producing the record", e);
                        }
                    }
                });
                try {
                    Thread.sleep(config.appDelayMs);
                } catch (InterruptedException exc) {exc.printStackTrace();}
                producer.flush();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

}
