package com.rh.dev;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.kafka.client.serialization.JsonbSerde;
import io.quarkus.logging.Log;

@ApplicationScoped
public class KStreamsEnricherTopology {

    public static record SensorData(
        int deviceId,
        Map<String,Double> measurements,
        LocalDateTime timestamp
    ) {};

    public static record DeviceData(
        int id,
        int active, 
        double latitude,
        double longitude,
        String brand
    ) {};

    public static record Location(String type, List<Double> coordinates) {}

    public static record EnrichedSensorData(
        int deviceId,
        String brand,
        boolean active,
        Location location,
        Map<String,Double> measurements,
        LocalDateTime timestamp
    ) {
        public EnrichedSensorData(int deviceId,String brand,boolean active,Location location,Map<String,Double> measurements,LocalDateTime timestamp) {
            this.deviceId=deviceId;
            this.brand=brand;
            this.active=active;
            this.location=location;
            this.measurements=measurements;
            this.timestamp=timestamp;
        }
        
        public EnrichedSensorData(DeviceData deviceData, SensorData sensorData) {
            this(
                deviceData.id,
                deviceData.brand,
                deviceData.active == 0 ? false : true,
                new Location("Point",List.of(deviceData.longitude,deviceData.latitude)),
                sensorData.measurements,
                sensorData.timestamp);
        }

    };

    @ConfigProperty(name = "app.topic.device")
    String appTopicDevices;

    @ConfigProperty(name = "app.topic.measurements")
    String appTopicMeasurements;

    @ConfigProperty(name = "app.topic.enriched")
    String appTopicEnriched;

    @Produces
    public Topology enrichMeasurementsStream() {
        
        final StreamsBuilder builder = new StreamsBuilder();
        
        final JsonbSerde<SensorData> sensorDataSerde = new JsonbSerde<>(SensorData.class);
        final JsonbSerde<DeviceData> deviceDataSerde = new JsonbSerde<>(DeviceData.class);
        final JsonbSerde<EnrichedSensorData> enrichedSerde = new JsonbSerde<>(EnrichedSensorData.class);

        final GlobalKTable<Integer, DeviceData> devices = builder.globalTable(
                appTopicDevices,
                Consumed.with(Serdes.Integer(), deviceDataSerde),
                Materialized.as("global-devices-lut")
        );

        builder.stream(
            appTopicMeasurements,
            Consumed.with(Serdes.Integer(), sensorDataSerde)
        )
        .peek((deviceId,data) -> Log.infof("sensor data for deviceId: %d -> %s", deviceId, data))
        .join(
            devices,
            (id, sensorData) -> id,
            (sensorData, deviceData) -> new EnrichedSensorData(deviceData,sensorData)
        )
        .peek((id, enriched) -> Log.infof("enriched sensor data for deviceId: %d -> %s", id, enriched))
        .to(appTopicEnriched, Produced.with(Serdes.Integer(), enrichedSerde));
        
        return builder.build();
    }

}