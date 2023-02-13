package com.rh.dev;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import net.datafaker.Faker;

@ApplicationScoped
public class IotStreamGenerator {

    public static record SensorData(
        int deviceId,
        Map<String,Double> measurements,
        @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime timestamp
    ) {};

    private static Faker DATA_FAKER = new Faker();

    @Outgoing("iot-measurements")
    public Multi<KafkaRecord<Integer, SensorData>> generateMeasurements() {
        Log.info("starting simulate random device measurments every ... ms");
        return Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .onOverflow().drop()
                .map(t -> new SensorData(DATA_FAKER.number().numberBetween(1001, 1011), 
                                Map.of(
                                    "sensor-a",DATA_FAKER.number().randomDouble(0, -10, 0),
                                    "sensor-b",DATA_FAKER.number().randomDouble(2, 0, 100),
                                    "sensor-c",DATA_FAKER.number().randomDouble(1, -50, 50),
                                    "sensor-d",DATA_FAKER.number().randomDouble(0, 90, 100),
                                    "sensor-e",DATA_FAKER.number().randomDouble(3, 0, 1)
                                ),LocalDateTime.now())
                )
                .invoke(sd -> Log.infof("producing sensor data -> %s", sd))
                .map(sd -> KafkaRecord.of(sd.deviceId,sd));
    }
    
}
