package com.rh.dev;

import java.util.Map;

import org.apache.kafka.common.serialization.Serde;

import com.bakdata.kafka.AbstractLargeMessageConfig;
import com.bakdata.kafka.LargeMessageSerde;
import com.bakdata.kafka.LargeMessageSerdeConfig;

public class SerdeUtil {

    static <T, S> Serde<T> createBackingSerdeS3(Configuration config, Class<T> wrappedType, Class<S> wrappedSerde, boolean isKey) {
        var s3backedSerde = new LargeMessageSerde<T>();
        s3backedSerde.configure(Map.of(
                AbstractLargeMessageConfig.BASE_PATH_CONFIG, config.serdeS3BasePath,
                AbstractLargeMessageConfig.S3_ENDPOINT_CONFIG, config.serdeS3EndPoint,
                AbstractLargeMessageConfig.S3_REGION_CONFIG, config.serdeS3Region,
                AbstractLargeMessageConfig.S3_ACCESS_KEY_CONFIG, config.serdeS3AccessKey,
                AbstractLargeMessageConfig.S3_SECRET_KEY_CONFIG, config.serdeS3SecretKey,
                AbstractLargeMessageConfig.USE_HEADERS_CONFIG, config.serdeS3WithHeaders,
                AbstractLargeMessageConfig.MAX_BYTE_SIZE_CONFIG, config.serdeS3MaxBytesSize,
                LargeMessageSerdeConfig.VALUE_SERDE_CLASS_CONFIG, wrappedSerde),
                isKey);
        return s3backedSerde;
    }
   
}
