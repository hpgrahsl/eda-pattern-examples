# Event-Driven Architecture Patterns

## Claim Check Pattern Example

The _claim check pattern_ pattern comes in very handy when applications need to communicate larger events to downstream consumers. The core idea is not to directly send larger objects (e.g. images, videos, documents, ...) as part of the payload across messaging or event streaming infrastructure. Instead, the large message is split into two parts:

1. **claim check:** some unique identifier to a database record or more appropriately a reference to an object in some storage service

2. **actual data:** the raw, often binary data which is offloaded to storage solutions such as Amazon S3, Azure Blob Storage, or Google Cloud Storage

The major benefit with this approach is that neither the messaging infrastructure nor the downstream client application are negatively impacted due to the direct transfer of large objects within event payloads. Additionally, it can be considerably more cost effective to decouple the storage layer from the processing layer when building data- and event-driven applications.

### Example Overview

The repository hosts a basic example which shows how to apply the _claim check pattern_ to send images from one application to another. Apache Kafka is used as the event streaming platform in between which allows to decouple this data flow. MinIO serves as an S3-compatible storage service and is used to store the actual binary data for all images. The illustration below shows a high-level overview:

![eda-ccp-overview.png](docs/eda-ccp-overview.png)

### Implementation Details

* Both, the producer and consumer are simple [Apache Kafka](https://kafka.apache.org) client applications which are implemented based on [Quarkus](https://quarkus.io).

* The actual work of externalizing the large message payloads - images in this example - is delegated to a specific Kafka [Serde implementation](https://github.com/bakdata/kafka-large-message-serde) which allows to send and retrieve data to and from storage services such as S3.

* [MinIO](https://min.io/) is used as self-hosted and S3 compatible storage service to store all the raw data for images

### How to run it?

There are two different ways to run this example scenario.

#### **Docker Compose**

In case you want to run this locally, simply go into the folder `eda-cpp/docker` and run `docker compose up` in your terminal. All components will start and after a couple of moments, you should see log output from the `eda-ccp-producer` and `eda-ccp-consumer` showing that images are produced to / consumed from a Kafka topic. Since the logs are interleaved in the main docker compose window, it's easier to inspect the logs in separate terminal windows as follows:

`docker compose logs eda-ccp-producer`

```log
...

eda-ccp-producer  | 2023-02-07 13:54:39,514 INFO  [com.rh.dev.ImageProducer] (main) start to produce records to topic my_image_stream_topic...
eda-ccp-producer  | 2023-02-07 13:54:39,517 INFO  [com.rh.dev.ImageProducer] (main) producing data for image: /home/data/images/dawson-lovell-W_MUqtuHwyY-unsplash.jpg
eda-ccp-producer  | 2023-02-07 13:54:42,466 INFO  [com.rh.dev.ImageProducer$1] (kafka-producer-network-thread | producer-1) meta data: Topic:my_image_stream_topic Partition: 0 Offset: 0 Timestamp: 1675778081204
eda-ccp-producer  | 2023-02-07 13:54:43,280 INFO  [com.rh.dev.ImageProducer] (main) producing data for image: /home/data/images/cristofer-maximilian-uQDRDqpYJHI-unsplash.jpg
eda-ccp-producer  | 2023-02-07 13:54:43,382 INFO  [com.rh.dev.ImageProducer$1] (kafka-producer-network-thread | producer-1) meta data: Topic:my_image_stream_topic Partition: 0 Offset: 1 Timestamp: 1675778083285
...
```

`docker compose logs eda-ccp-consumer`

```log
...

eda-ccp-consumer  | 2023-02-07 13:54:39,417 INFO  [com.rh.dev.ImageConsumer] (main) start to consume records from topic my_image_stream_topic...
eda-ccp-consumer  | 2023-02-07 13:54:43,349 INFO  [com.rh.dev.ImageConsumer] (main) consumed image with key /home/data/images/dawson-lovell-W_MUqtuHwyY-unsplash.jpg -> 'execute business logic...'
eda-ccp-consumer  | 2023-02-07 13:54:45,150 INFO  [com.rh.dev.ImageConsumer] (main) IMAGE META DATA -> width: 1330 | height: 1980 | size: 768426
eda-ccp-consumer  | 2023-02-07 13:54:45,222 INFO  [com.rh.dev.ImageConsumer] (main) consumed image with key /home/data/images/cristofer-maximilian-uQDRDqpYJHI-unsplash.jpg -> 'execute business logic...'
eda-ccp-consumer  | 2023-02-07 13:54:46,599 INFO  [com.rh.dev.ImageConsumer] (main) IMAGE META DATA -> width: 1320 | height: 1980 | size: 417013
...
```

In contrast, any Kafka client which hasn't been configured to use the specific Serde implementation only sees the storage reference in the consumed Kafka record's value. By checking the logs for the standard `kafka-console-consumer` this becomes evident:

`docker compose logs console-consumer`

```log
...

console-consumer  | /home/data/images/dawson-lovell-W_MUqtuHwyY-unsplash.jpg -> s3://eda-ccp-s3/my_image_stream_topic/values/3cf67bd1-fdc9-49f1-beca-1eca7ce27af8
console-consumer  | /home/data/images/cristofer-maximilian-uQDRDqpYJHI-unsplash.jpg -> s3://eda-ccp-s3/my_image_stream_topic/values/0dc19216-162c-430d-b9a9-297b080d7cec
...
```


#### **Kubernetes**

In case you want to run this example in a Kubernetes environment, there are ready-made YAML manifests you can directly apply to your k8s cluster.

1. Make sure your `kubectl` context is configured properly. 
2. Then go into the main folder of the example `eda-cpp` and simply run `kubectl apply -f k8s` in your terminal.
3. All contained `.yaml` files - for infra and app components -  will get deployed into your configured cluster.

After a few moments, everything should be up and running fine:

`kubectl get pods`

```bash
NAME                                READY   STATUS      RESTARTS   AGE
console-consumer-67bfc979c4-rqxs4   1/1     Running     0          62s
eda-ccp-consumer-9bf7f9978-9crwt    1/1     Running     0          62s
eda-ccp-producer-76b4f975c4-x9f9p   1/1     Running     0          62s
kafka-67cbf4cd74-wlnpm              1/1     Running     0          60s
minio-6757448989-2n4fc              1/1     Running     0          58s
minio-bucket-init-f8bkl             0/1     Completed   0          58s
zookeeper-59f5fb6dff-xtc8b          1/1     Running     0          61s
```

By checking the logs, you can inspect what happens in the applications. **Note, that you need to change the pod names accordingly** to match those reflected in your environment:

`kubectl logs -f eda-ccp-producer-76b4f975c4-x9f9p`

```log
...

2023-02-07 14:21:01,050 INFO  [com.rh.dev.ImageProducer] (main) producing data for image: /home/data/images/sebastian-unrau-sp-p7uuT0tw-unsplash.jpg
2023-02-07 14:21:01,076 INFO  [com.rh.dev.ImageProducer$1] (kafka-producer-network-thread | producer-1) meta data: Topic:my_image_stream_topic Partition: 0 Offset: 1 Timestamp: 1675779661051
2023-02-07 14:21:02,570 INFO  [com.rh.dev.ImageProducer] (main) producing data for image: /home/data/images/cristofer-maximilian-uQDRDqpYJHI-unsplash.jpg
2023-02-07 14:21:02,592 INFO  [com.rh.dev.ImageProducer$1] (kafka-producer-network-thread | producer-1) meta data: Topic:my_image_stream_topic Partition: 0 Offset: 2 Timestamp: 1675779662575
...
```

`kubectl logs -f eda-ccp-consumer-9bf7f9978-9crwt`

```log
...

2023-02-07 14:21:00,677 INFO  [com.rh.dev.ImageConsumer] (main) consumed image with key /home/data/images/josh-hild-_TuI8tZHlk4-unsplash.jpg -> 'execute business logic...'
2023-02-07 14:21:01,333 INFO  [com.rh.dev.ImageConsumer] (main) IMAGE META DATA -> width: 1584 | height: 1980 | size: 562255
2023-02-07 14:21:01,365 INFO  [com.rh.dev.ImageConsumer] (main) consumed image with key /home/data/images/sebastian-unrau-sp-p7uuT0tw-unsplash.jpg -> 'execute business logic...'
2023-02-07 14:21:01,792 INFO  [com.rh.dev.ImageConsumer] (main) IMAGE META DATA -> width: 1980 | height: 1320 | size: 571989
...
``` 

In contrast, any Kafka client which hasn't been configured to use the specific Serde implementation only sees the storage reference in the consumed Kafka record's value. By checking the logs for the standard `kafka-console-consumer` this becomes evident:

`kubectl logs -f console-consumer-67bfc979c4-rqxs4`

```log
...

/home/data/images/josh-hild-_TuI8tZHlk4-unsplash.jpg -> s3://eda-ccp-s3/my_image_stream_topic/values/40810d79-9e91-4520-ae0b-5fa1c769726e
/home/data/images/sebastian-unrau-sp-p7uuT0tw-unsplash.jpg -> s3://eda-ccp-s3/my_image_stream_topic/values/3befcabc-666d-4498-8cda-59405cf3f3cc
...
```

## Content Enricher Pattern Example

In event-driven architectures, services communicate by means of different types of events. When producers send so-called notification events - which by definition contain a rather compact payload  - consumers oftentimes have the need to obtain additional information. In particular, whenever event payloads contain references to domain entities (e.g. userID, productID, orderID, ...) consumers might want to get additional data describing the referenced entity. The _content enricher pattern_ helps with making sure that event consumers have all the data they need, without them explicitly calling back to the producing service or reaching out to other services directly to fetch the data in question.

### Example Overview

The repository hosts a basic example which shows how to apply the _content enricher pattern_ when processing sensor data. The raw sensor data only contains an identifier for a particular device, however certain downstream consumers want to operate on additional device data. For that purpose, the device data needs to be ingested into Apache Kafka so that a stream processing application can join the raw sensor data with the device data. The illustration below shows a high-level overview:

![eda-ccp-overview.png](docs/eda-cep-overview.png)

### Implementation Details

* The device data resides in a MySQL database table. This data is ingested into Apache Kafka by means of change-data-capture using the [Debezium MySQL source connector](https://debezium.io/documentation/reference/stable/connectors/mysql.html).

* Both applications, the producer app and the enricher app are written with [Quarkus](https://quarkus.io). The producer app simulates a stream of random sensor data for 10 different device IDs. The enricher app consumes the device data which originates from the Debezium CDC-stream into a global table that is backed by a state store and essentially serves as lookup table. It also consumes the stream of raw sensor data records and joins each record against the device data based on the device id. All join results are continuously written back into a separate topic, which allows downstream consumers to directly operate on the enriched data.

* The downstream consumer in this case is a [MongoDB sink connector](https://www.mongodb.com/docs/kafka-connector/current/) which writes the enriched data into a timeseries collection for data analytics purposes.

### How to run it?

There are two different ways to run this example scenario.

#### **Docker Compose**

In case you want to run this locally, simply go into the folder 
`eda-cep/docker` and run `docker compose up` in your terminal. All components will start and after a couple of moments, you should see log output from the `eda-cep-generator` and `eda-cep-enricher` showing that raw sensor data records are consumed and joined against the device data to produce enriched sensor data. Since the logs are interleaved in the main docker compose window, it's easier to inspect the logs in separate terminal windows as follows:

`docker compose logs eda-cep-generator`

```log
...

eda-cep-generator  | 2023-02-13 10:48:43,200 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1010, measurements={sensor-a=-9.0, sensor-b=98.54, sensor-c=35.7, sensor-d=92.0, sensor-e=0.563}, timestamp=2023-02-13T10:48:43.199950634]
eda-cep-generator  | 2023-02-13 10:48:44,437 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1007, measurements={sensor-a=-1.0, sensor-b=89.79, sensor-c=-45.6, sensor-d=97.0, sensor-e=0.907}, timestamp=2023-02-13T10:48:44.436796302]
eda-cep-generator  | 2023-02-13 10:48:45,137 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1004, measurements={sensor-a=-8.0, sensor-b=80.67, sensor-c=30.5, sensor-d=98.0, sensor-e=0.522}, timestamp=2023-02-13T10:48:45.137402510]
...
```

`docker compose logs eda-cep-enricher`

```log 
...
eda-cep-enricher  | 2023-02-13 10:48:51,758 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) sensor data for deviceId: 1010 -> SensorData[deviceId=1010, measurements={sensor-a=-9.0, sensor-b=98.54, sensor-e=0.563, sensor-c=35.7, sensor-d=92.0}, timestamp=2023-02-13T10:48:43.199]
eda-cep-enricher  | 2023-02-13 10:48:51,910 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) enriched sensor data for deviceId: 1010 -> EnrichedSensorData[deviceId=1010, brand=Telit, active=false, location=Location[type=Point, coordinates=[-71.05977, 42.35843]], measurements={sensor-a=-9.0, sensor-b=98.54, sensor-e=0.563, sensor-c=35.7, sensor-d=92.0}, timestamp=2023-02-13T10:48:43.199]
eda-cep-enricher  | 2023-02-13 10:48:52,093 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) sensor data for deviceId: 1007 -> SensorData[deviceId=1007, measurements={sensor-a=-1.0, sensor-b=89.79, sensor-e=0.907, sensor-c=-45.6, sensor-d=97.0}, timestamp=2023-02-13T10:48:44.436]
eda-cep-enricher  | 2023-02-13 10:48:52,095 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) enriched sensor data for deviceId: 1007 -> EnrichedSensorData[deviceId=1007, brand=SoluLab, active=true, location=Location[type=Point, coordinates=[-78.63861, 35.7721]], measurements={sensor-a=-1.0, sensor-b=89.79, sensor-e=0.907, sensor-c=-45.6, sensor-d=97.0}, timestamp=2023-02-13T10:48:44.436]
eda-cep-enricher  | 2023-02-13 10:48:52,103 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) sensor data for deviceId: 1004 -> SensorData[deviceId=1004, measurements={sensor-a=-8.0, sensor-b=80.67, sensor-e=0.522, sensor-c=30.5, sensor-d=98.0}, timestamp=2023-02-13T10:48:45.137]
eda-cep-enricher  | 2023-02-13 10:48:52,105 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-2456e06a-b8f7-43cf-a4c3-1a8298ab8201-StreamThread-1) enriched sensor data for deviceId: 1004 -> EnrichedSensorData[deviceId=1004, brand=GE Digital, active=true, location=Location[type=Point, coordinates=[2.15899, 41.38879]], measurements={sensor-a=-8.0, sensor-b=80.67, sensor-e=0.522, sensor-c=30.5, sensor-d=98.0}, timestamp=2023-02-13T10:48:45.137]
...
```

To check the enriched data in the MongoDB collection, exec into the MongoDB shell inside the container with `docker compose exec mongodb mongosh` and run a simple query to show one sample document in the timeseries collection:

```mongosh
use iot_db;
db.getCollection('sensors-ts').findOne();
```

which should give you a document similar to the following

```
{
  timestamp: ISODate("2023-02-13T10:48:43.199Z"),
  deviceId: Long("1010"),
  active: false,
  brand: 'Telit',
  measurements: {
    'sensor-a': -9,
    'sensor-b': 98.54,
    'sensor-e': 0.563,
    'sensor-c': 35.7,
    'sensor-d': 92
  },
  location: { coordinates: [ -71.05977, 42.35843 ], type: 'Point' },
  _id: ObjectId("63ea15946f814d6ae0d34af9")
}
```

#### **Kubernetes**

In case you want to run this example in a Kubernetes environment, there are ready-made YAML manifests you can directly apply to your k8s cluster.

1. Make sure your `kubectl` context is configured properly. 
2. Then go into the main folder of the example `eda-cep` and simply run `kubectl apply -f k8s` in your terminal.
3. All contained `.yaml` files - for infra and app components -  will get deployed into your configured cluster.

After a few moments, everything should be up and running fine:

`kubectl get pods`

```bash
NAME                                READY   STATUS      RESTARTS   AGE
connect-5dbc4fcf7c-j5v62            1/1     Running     0          48s
connectors-creator-tpv6w            0/1     Completed   0          48s
eda-cep-enricher-5b4867dd5f-f6c6f   1/1     Running     0          45s
eda-cep-generator-fc9d5d6b-tvxdz    1/1     Running     0          46s
kafka-67cbf4cd74-vwswr              1/1     Running     0          44s
mongodb-b768c557-dn5w2              1/1     Running     0          43s
mysql-76f5fc46cc-z8762              1/1     Running     0          42s
zookeeper-59f5fb6dff-gzggr          1/1     Running     0          45s
```

By checking the logs, you can inspect what happens in the applications. **Note, that you need to change the pod names accordingly** to match those reflected in your environment:

`kubectl logs -f eda-cep-generator-fc9d5d6b-tvxdz`

```log
...

2023-02-13 11:03:50,006 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1009, measurements={sensor-e=0.302, sensor-d=94.0, sensor-c=32.8, sensor-b=61.32, sensor-a=-5.0}, timestamp=2023-02-13T11:03:50.005956047]
2023-02-13 11:03:51,002 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1006, measurements={sensor-e=0.517, sensor-d=92.0, sensor-c=-24.1, sensor-b=55.15, sensor-a=-6.0}, timestamp=2023-02-13T11:03:51.002677142]
2023-02-13 11:03:52,002 INFO  [com.rh.dev.IotStreamGenerator] (executor-thread-0) producing sensor data -> SensorData[deviceId=1008, measurements={sensor-e=0.813, sensor-d=92.0, sensor-c=-19.3, sensor-b=3.96, sensor-a=-4.0}, timestamp=2023-02-13T11:03:52.002727776]
...
```

`kubectl logs -f eda-cep-enricher-5b4867dd5f-f6c6f`

```log
...

2023-02-13 11:04:18,874 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) sensor data for deviceId: 1009 -> SensorData[deviceId=1009, measurements={sensor-a=-5.0, sensor-b=61.32, sensor-e=0.302, sensor-c=32.8, sensor-d=94.0}, timestamp=2023-02-13T11:03:50.005]
2023-02-13 11:04:18,891 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) enriched sensor data for deviceId: 1009 -> EnrichedSensorData[deviceId=1009, brand=Cisco, active=true, location=Location[type=Point, coordinates=[-74.0, 40.7166638]], measurements={sensor-a=-5.0, sensor-b=61.32, sensor-e=0.302, sensor-c=32.8, sensor-d=94.0}, timestamp=2023-02-13T11:03:50.005]
2023-02-13 11:04:18,999 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) sensor data for deviceId: 1006 -> SensorData[deviceId=1006, measurements={sensor-a=-6.0, sensor-b=55.15, sensor-e=0.517, sensor-c=-24.1, sensor-d=92.0}, timestamp=2023-02-13T11:03:51.002]
2023-02-13 11:04:19,000 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) enriched sensor data for deviceId: 1006 -> EnrichedSensorData[deviceId=1006, brand=Verizon, active=false, location=Location[type=Point, coordinates=[-0.12574, 51.50853]], measurements={sensor-a=-6.0, sensor-b=55.15, sensor-e=0.517, sensor-c=-24.1, sensor-d=92.0}, timestamp=2023-02-13T11:03:51.002]
2023-02-13 11:04:19,002 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) sensor data for deviceId: 1008 -> SensorData[deviceId=1008, measurements={sensor-a=-4.0, sensor-b=3.96, sensor-e=0.813, sensor-c=-19.3, sensor-d=92.0}, timestamp=2023-02-13T11:03:52.002]
2023-02-13 11:04:19,002 INFO  [com.rh.dev.KStreamsEnricherTopology] (enricher-app-001-650c2967-c8c0-4ade-8a61-f27d05c4603c-StreamThread-1) enriched sensor data for deviceId: 1008 -> EnrichedSensorData[deviceId=1008, brand=Telit, active=false, location=Location[type=Point, coordinates=[-122.41942, 37.77493]], measurements={sensor-a=-4.0, sensor-b=3.96, sensor-e=0.813, sensor-c=-19.3, sensor-d=92.0}, timestamp=2023-02-13T11:03:52.002]
...
``` 

To check the enriched data in the MongoDB collection, exec into the MongoDB shell inside the pod's container (_NOTE: adapt the pod's name accordingly_) with `kubectl exec -it mongodb-b768c557-dn5w2 -- mongosh` and run a simple query to show one sample document in the timeseries collection:

```mongosh
use iot_db;
db.getCollection('sensors-ts').findOne();
```

which should give you a document similar to the following

```
{
  timestamp: ISODate("2023-02-13T11:03:50.005Z"),
  deviceId: Long("1009"),
  active: true,
  _id: ObjectId("63ea19330d43246cd9a6c3a2"),
  brand: 'Cisco',
  location: { coordinates: [ -74, 40.7166638 ], type: 'Point' },
  measurements: {
    'sensor-a': -5,
    'sensor-b': 61.32,
    'sensor-e': 0.302,
    'sensor-c': 32.8,
    'sensor-d': 94
  }
}
```

## Message Translator Pattern Example

The _message translator pattern_ helps to reduce the coupling between event producers and consumers. Two reasons for message translations which can be applied in-flight are: a) to integrate with legacy systems and their proprietary/non-standardized data formats or b) to avoid leaking the domain models in microservice architectures. Translations of events come in many different flavours, e.g. data type conversions, date/time format harmonization, field redaction, name changes, payload format conversions, etc. While the actual translation of events can be implemented in different ways, it is oftentimes beneficial to do this outside of the producing/consuming application's context.

### Example Overview

The repository hosts a basic example which shows how to apply the _message translator pattern_ when processing point-of-sales data. The raw data comes in CSV format and resides on some disk-based storage. The CSV records need to be sent to a web service which expects data in a specific JSON format. For that purpose, Kafka Connect is used to define a streaming data pipeline which translates the original point-of-sales data in-flight. The illustration below shows a high-level overview:

![eda-mtp-overview.png](docs/eda-mtp-overview.png)

### Implementation Details

* For simplicity reasons, the raw input data (`pos_transactions.csv`) is mounted directly into a folder on the Kafka Connect worker node. In general, the data could be read from different internal/external storage systems.

* A Kafka Connect [file source connector](https://github.com/streamthoughts/kafka-connect-file-pulse) is used to produce the csv records into a Kafka topic. In order to match the expected JSON format from the targeted web service, each record is translated on the fly by means of additional configuration which does the following:
  - convert selected string fields to numeric data types
  - drop unnecessary fields and rename other fields accordingly
  - serialize records into the expected JSON format

* A Kafka Connect [http sink connector](https://github.com/aiven/http-connector-for-apache-kafka/) is used to consume and send the data to the web service which is supposed to receive and process the point-of-sales data.

### How to run it?

There are two different ways to run this example scenario.

#### **Docker Compose**

In case you want to run this locally, simply go into the folder 
`eda-mtp/docker` and run `docker compose up` in your terminal. All components will start and after a couple of moments, you should see log output related to the configured Kafka Connect streaming data pipeline. As soon as both, the file source and http sink connectors are started and running, the web service `eda-mtp-consumer` will receive the translated point-of-sales data in batches of up to 10 records each.

1. The `csv` input data can be inspected like so:

`docker compose exec -it connect head /home/files/pos_transactions.csv`

```csv
WorkstationGroupID,TranID,BeginDateTime,EndDateTime,OperatorID,TranTime,BreakTime,ArtNum,TNcash,TNcard,Amount
1,19040310601356,2019-04-03T13:28:17,2019-04-03T13:28:17,10,0,1,1,FALSE,FALSE,11.99
1,19032910607385,2019-03-29T14:04:50,2019-03-29T14:05:31,101,41,21,6,TRUE,FALSE,13.74
1,19032910607386,2019-03-29T14:05:52,2019-03-29T14:06:35,101,43,44,12,TRUE,FALSE,97.47
1,19032910607387,2019-03-29T14:07:19,2019-03-29T14:07:43,101,24,25,6,FALSE,TRUE,37.18
1,19032910607389,2019-03-29T14:08:08,2019-03-29T14:09:44,101,96,18,48,TRUE,FALSE,154.7
1,19032910607390,2019-03-29T14:10:02,2019-03-29T14:10:02,101,0,270,22,FALSE,FALSE,108.06
1,19032910607398,2019-03-29T14:14:32,2019-03-29T14:14:45,101,13,33,5,TRUE,FALSE,19.36
1,19032910607399,2019-03-29T14:15:18,2019-03-29T14:15:49,101,31,71,5,TRUE,FALSE,16.26
1,19032910607401,2019-03-29T14:17:00,2019-03-29T14:17:57,101,57,45,25,FALSE,TRUE,60.39
```

2. The Kafka Connect pipeline translates this data on the fly into the expected JSON format. The records in the corresponding Kafka topic `pos-transactions` look as follows:

`docker compose exec -it kafka bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic pos-transactions --from-beginning`

```json5
...

{"total-amount":23.89,"items-count":9,"datetime":"2019-04-04T12:48:14","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022306}
{"total-amount":141.08,"items-count":22,"datetime":"2019-04-04T12:50:21","operator-id":112,"with-card":false,"with-cash":true,"id":190404106022308}
{"total-amount":39.9,"items-count":10,"datetime":"2019-04-04T12:51:03","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022309}
{"total-amount":106.3,"items-count":25,"datetime":"2019-04-04T12:52:57","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022311}
{"total-amount":27.49,"items-count":6,"datetime":"2019-04-04T12:53:30","operator-id":112,"with-card":false,"with-cash":true,"id":190404106022313}
```

3. The logs of the `eda-mtp-consumer` application show that the point-of-sales data is retrieved as a result of the http sink connector pushing the kafka records in batches of max. 10 records to the API endpoint:

`docker compose logs -f eda-mtp-consumer`

```log
...

eda-mtp-consumer  | 2023-02-20 10:34:21,541 INFO  [io.quarkus] (main) consumer-app 0.1.0 on JVM (powered by Quarkus 2.16.3.Final) started in 8.692s. Listening on: http://0.0.0.0:8080
eda-mtp-consumer  | 2023-02-20 10:34:21,735 INFO  [io.quarkus] (main) Profile prod activated. 
eda-mtp-consumer  | 2023-02-20 10:34:21,736 INFO  [io.quarkus] (main) Installed features: [cdi, kubernetes, resteasy-reactive, resteasy-reactive-jackson, smallrye-context-propagation, vertx]
eda-mtp-consumer  | 2023-02-20 10:34:39,02

...

eda-mtp-consumer  | 2023-02-20 10:34:47,545 DEBUG [com.rh.dev.PosDataResource] (executor-thread-1) 10 transaction(s) received: [TransactionData[id=190404106022278, dateTime=2019-04-04T12:18:46, itemsCount=20, totalAmount=34.14, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022280, dateTime=2019-04-04T12:21:44, itemsCount=30, totalAmount=136.88, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022282, dateTime=2019-04-04T12:25:28, itemsCount=79, totalAmount=384.34, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022284, dateTime=2019-04-04T12:27:22, itemsCount=25, totalAmount=95.25, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022286, dateTime=2019-04-04T12:28:58, itemsCount=28, totalAmount=112.09, operatorId=112, withCard=false, withCash=false], TransactionData[id=190404106022287, dateTime=2019-04-04T12:29:29, itemsCount=5, totalAmount=24.95, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022288, dateTime=2019-04-04T12:30:18, itemsCount=8, totalAmount=34.06, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022289, dateTime=2019-04-04T12:31:27, itemsCount=6, totalAmount=23.77, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022291, dateTime=2019-04-04T12:33:14, itemsCount=12, totalAmount=122.28, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022292, dateTime=2019-04-04T12:35:06, itemsCount=19, totalAmount=48.65, operatorId=112, withCard=true, withCash=false]]
eda-mtp-consumer  | 2023-02-20 10:34:47,549 DEBUG [com.rh.dev.PosDataResource] (executor-thread-1) 10 transaction(s) received: [TransactionData[id=190404106022294, dateTime=2019-04-04T12:36:09, itemsCount=5, totalAmount=5.32, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022295, dateTime=2019-04-04T12:38:01, itemsCount=25, totalAmount=104.83, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022296, dateTime=2019-04-04T12:40:15, itemsCount=35, totalAmount=327.31, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022298, dateTime=2019-04-04T12:41:14, itemsCount=9, totalAmount=48.98, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022299, dateTime=2019-04-04T12:42:14, itemsCount=17, totalAmount=59.11, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022300, dateTime=2019-04-04T12:43:08, itemsCount=11, totalAmount=43.89, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022301, dateTime=2019-04-04T12:43:46, itemsCount=2, totalAmount=17.22, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022302, dateTime=2019-04-04T12:45:32, itemsCount=20, totalAmount=64.13, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022304, dateTime=2019-04-04T12:47:41, itemsCount=45, totalAmount=163.24, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022306, dateTime=2019-04-04T12:48:14, itemsCount=9, totalAmount=23.89, operatorId=112, withCard=true, withCash=false]]
eda-mtp-consumer  | 2023-02-20 10:34:47,556 DEBUG [com.rh.dev.PosDataResource] (executor-thread-1) 4 transaction(s) received: [TransactionData[id=190404106022308, dateTime=2019-04-04T12:50:21, itemsCount=22, totalAmount=141.08, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022309, dateTime=2019-04-04T12:51:03, itemsCount=10, totalAmount=39.9, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022311, dateTime=2019-04-04T12:52:57, itemsCount=25, totalAmount=106.3, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022313, dateTime=2019-04-04T12:53:30, itemsCount=6, totalAmount=27.49, operatorId=112, withCard=false, withCash=true]]
...
```

#### **Kubernetes**

In case you want to run this example in a Kubernetes environment, there are ready-made YAML manifests you can directly apply to your k8s cluster.

1. Make sure your `kubectl` context is configured properly. 
2. Then go into the main folder of the example `eda-mtp` and simply run `kubectl apply -f k8s` in your terminal.
3. All contained `.yaml` files - for infra and app components - will get deployed into your configured cluster.

After a few moments, everything should be up and running fine:

`kubectl get pods`

```bash
NAME                                READY   STATUS      RESTARTS   AGE
connect-749cb9cfc4-2scwr            1/1     Running     0          55s
connectors-creator-28hkv            0/1     Completed   0          55s
eda-mtp-consumer-69f5cc8785-tzfnx   1/1     Running     0          53s
kafka-67cbf4cd74-rfjzd              1/1     Running     0          51s
zookeeper-59f5fb6dff-m4zk5          1/1     Running     0          52s
```

As soon as both, the file source and http sink connectors are started and running, the web service `eda-mtp-consumer` will receive the translated point-of-sales data in batches of up to 10 records each.

NOTE: you need to adapt the pod names accordingly for any of the following `kubectl` commands.

1. The `csv` input data can be inspected like so:

`kubectl exec -it connect-749cb9cfc4-2scwr -- head /home/pos_transactions.csv`

```csv
WorkstationGroupID,TranID,BeginDateTime,EndDateTime,OperatorID,TranTime,BreakTime,ArtNum,TNcash,TNcard,Amount
1,19040310601356,2019-04-03T13:28:17,2019-04-03T13:28:17,10,0,1,1,FALSE,FALSE,11.99
1,19032910607385,2019-03-29T14:04:50,2019-03-29T14:05:31,101,41,21,6,TRUE,FALSE,13.74
1,19032910607386,2019-03-29T14:05:52,2019-03-29T14:06:35,101,43,44,12,TRUE,FALSE,97.47
1,19032910607387,2019-03-29T14:07:19,2019-03-29T14:07:43,101,24,25,6,FALSE,TRUE,37.18
1,19032910607389,2019-03-29T14:08:08,2019-03-29T14:09:44,101,96,18,48,TRUE,FALSE,154.7
1,19032910607390,2019-03-29T14:10:02,2019-03-29T14:10:02,101,0,270,22,FALSE,FALSE,108.06
1,19032910607398,2019-03-29T14:14:32,2019-03-29T14:14:45,101,13,33,5,TRUE,FALSE,19.36
1,19032910607399,2019-03-29T14:15:18,2019-03-29T14:15:49,101,31,71,5,TRUE,FALSE,16.26
1,19032910607401,2019-03-29T14:17:00,2019-03-29T14:17:57,101,57,45,25,FALSE,TRUE,60.39
```

2. The Kafka Connect pipeline translates this data on the fly into the expected JSON format. The records in the corresponding Kafka topic `pos-transactions` look as follows:

`kubectl exec -it kafka-67cbf4cd74-rfjzd -- bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic pos-transactions --from-beginning`

```json5
...

{"total-amount":23.89,"items-count":9,"datetime":"2019-04-04T12:48:14","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022306}
{"total-amount":141.08,"items-count":22,"datetime":"2019-04-04T12:50:21","operator-id":112,"with-card":false,"with-cash":true,"id":190404106022308}
{"total-amount":39.9,"items-count":10,"datetime":"2019-04-04T12:51:03","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022309}
{"total-amount":106.3,"items-count":25,"datetime":"2019-04-04T12:52:57","operator-id":112,"with-card":true,"with-cash":false,"id":190404106022311}
{"total-amount":27.49,"items-count":6,"datetime":"2019-04-04T12:53:30","operator-id":112,"with-card":false,"with-cash":true,"id":190404106022313}
```

3. The logs of the `eda-mtp-consumer` application show that the point-of-sales data is retrieved as a result of the http sink connector pushing the kafka records in batches of max. 10 records to the API endpoint:

`kubectl logs -f eda-mtp-consumer-69f5cc8785-tzfnx`

```log
2023-02-20 12:04:13,564 INFO [io.quarkus] (main) consumer-app 0.1.0 on JVM (powered by Quarkus 2.16.3.Final) started in 1.665s. Listening on: http://0.0.0.0:8080
2023-02-20 12:04:13,642 INFO [io.quarkus] (main) Profile prod activated.
2023-02-20 12:04:13,642 INFO [io.quarkus] (main) Installed features: [cdi, kubernetes, resteasy-reactive, resteasy-reactive-jackson, smallrye-context-propagation, vertx]

...
2023-02-20 12:04:47,268 DEBUG [com.rh.dev.PosDataResource] (executor-thread-0) 10 transaction(s) received: [TransactionData[id=190404106022282, dateTime=2019-04-04T12:25:28, itemsCount=79, totalAmount=384.34, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022284, dateTime=2019-04-04T12:27:22, itemsCount=25, totalAmount=95.25, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022286, dateTime=2019-04-04T12:28:58, itemsCount=28, totalAmount=112.09, operatorId=112, withCard=false, withCash=false], TransactionData[id=190404106022287, dateTime=2019-04-04T12:29:29, itemsCount=5, totalAmount=24.95, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022288, dateTime=2019-04-04T12:30:18, itemsCount=8, totalAmount=34.06, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022289, dateTime=2019-04-04T12:31:27, itemsCount=6, totalAmount=23.77, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404...
2023-02-20 12:04:47,269 DEBUG [com.rh.dev.PosDataResource] (executor-thread-0) 10 transaction(s) received: [TransactionData[id=190404106022296, dateTime=2019-04-04T12:40:15, itemsCount=35, totalAmount=327.31, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022298, dateTime=2019-04-04T12:41:14, itemsCount=9, totalAmount=48.98, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022299, dateTime=2019-04-04T12:42:14, itemsCount=17, totalAmount=59.11, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022300, dateTime=2019-04-04T12:43:08, itemsCount=11, totalAmount=43.89, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022301, dateTime=2019-04-04T12:43:46, itemsCount=2, totalAmount=17.22, operatorId=112, withCard=false, withCash=true], TransactionData[id=190404106022302, dateTime=2019-04-04T12:45:32, itemsCount=20, totalAmount=64.13, operatorId=112, withCard=true, withCash=false], TransactionData[id=1904041...
2023-02-20 12:04:47,271 DEBUG [com.rh.dev.PosDataResource] (executor-thread-0) 2 transaction(s) received: [TransactionData[id=190404106022311, dateTime=2019-04-04T12:52:57, itemsCount=25, totalAmount=106.3, operatorId=112, withCard=true, withCash=false], TransactionData[id=190404106022313, dateTime=2019-04-04T12:53:30, itemsCount=6, totalAmount=27.49, operatorId=112, withCard=false, withCash=true]]
...
```

