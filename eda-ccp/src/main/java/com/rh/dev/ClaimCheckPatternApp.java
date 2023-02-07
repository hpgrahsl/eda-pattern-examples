package com.rh.dev;

import javax.inject.Inject;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ClaimCheckPatternApp implements QuarkusApplication {

    @Inject
    ImageProducer producer;

    @Inject
    ImageConsumer consumer;

    @Override
    public int run(String... args) throws Exception {
        if(args.length != 1 || !args[0].equals("producer") && !args[0].equals("consumer")) {
            System.err.println("usage: application <producer|consumer>");
            System.exit(-1);
        }

        if(args[0].equals("producer")) {
            producer.produceRecords();
        } else {
            consumer.consumeRecords();
        }

        return 0;
    }

}
