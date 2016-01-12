package com.obzen.cep;

import com.obzen.cep.core.NodeServer;
import com.obzen.cep.core.definition.StartupOptions;
import com.obzen.cep.core.util.JsonUtil;
import com.obzen.common.event.ExtEvent;
import com.obzen.common.kafka.KafkaTopicDisruptReader;
import com.obzen.common.kafka.KafkaTopicReader;
import com.obzen.common.serializer.FieldType;
import com.obzen.common.serializer.impl.ExternalEventSerializer;
import com.obzen.common.util.ValueWaiter;
import io.vertx.core.json.JsonObject;
//import io.vertx.ext.unit.Async;
//import io.vertx.ext.unit.TestContext;
//import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

//@RunWith(VertxUnitRunner.class)
public class SampleTest {
    private static NodeServer server;
    private static Vertx vertx;
    private static String deploymentID;
    final float sampleValue = 240;

    private ExtEvent[] createEvents() {
        return new ExtEvent[]{
                new ExtEvent(System.currentTimeMillis(), new Object[]{"IBM", Float.valueOf(sampleValue), "DUMMY"})
        };
    }

    @Test
    //public void eventFeedTest(TestContext testCtx) throws Exception {
    public void eventFeedTest() throws Exception {
        final int feedCount = 10000;

        // Prepare event serializers
        ExternalEventSerializer outSiddhiEventSerializer = ExternalEventSerializer.builder()
                .addDataFieldType(FieldType.STRING)
                .addDataFieldType(FieldType.FLOAT)
                .addDataFieldType(FieldType.OBJECT)
                .build();
        ExternalEventSerializer inSiddhiEventSerializer = ExternalEventSerializer.builder()
                .addDataFieldType(FieldType.STRING)
                .addDataFieldType(FieldType.DOUBLE)
                //.addDataFieldType(FieldType.FLOAT)
                .addDataFieldType(FieldType.OBJECT)
                .build();


        String groupId = "ocep-kafka-group";
        String clientId = "ocep-kafka-client";
        //String brokerHost = "192.168.10.82";
        //int brokerPort = 9092;


        // Produce events
        String outTopic = "sampleInEventTopic";
        Properties p_props = new Properties();
        p_props.put("bootstrap.servers", "192.168.10.82:9092");
        p_props.put("acks", "1");
        p_props.put("buffer.memory", "33554432");
        p_props.put("batch.size", "16384");
        p_props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p_props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        p_props.put("client.id", clientId);
        KafkaProducer<Void, byte[]> producer = new KafkaProducer<>(p_props);

        for (int i = 0; i < feedCount; i++) {
            try {
                ExtEvent[] events = createEvents();
                System.out.println("SEND: " + Arrays.deepToString(events));
                byte[] eventsBytes = outSiddhiEventSerializer.serializeArray(events);
                producer.send(new ProducerRecord<Void, byte[]>(outTopic, eventsBytes));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        producer.close();

        //
        //Async inEventsAsync = testCtx.async();
        final AtomicBoolean found = new AtomicBoolean(false);
        final ValueWaiter<Double> valueReceiveWaiter = new ValueWaiter<Double>(Double.valueOf(feedCount * sampleValue)) {
            @Override
            public void whenFound() {
                found.set(true);
            }
        };

    //    // Consume events
    //    String inTopic = "sampleOutName0";
    //    KafkaTopicReader topicReader = new KafkaTopicDisruptReader(inTopic) {
    //        @Override
    //        public void processEventsBytes(long currentOffset, byte[] bytes) {
    //            try {
    //                ExtEvent[] events = inSiddhiEventSerializer.deserializeArray(bytes);
    //                System.out.println("RECV: " + Arrays.deepToString(events));
    //                for (int i = 0; i < events.length; i++) {
    //                    valueReceiveWaiter.checkValue((Double) events[i].getData()[1]);
    //                }
    //            } catch (IOException e) {
    //                throw new RuntimeException(e);
    //            }
    //        }
    //    };

    //    long readOffset = topicReader.findReadOffset();
    //    System.out.println("Offset read: " + readOffset);
    //    long nextOffset = -1L;
    //    while (true) {
    //        nextOffset = topicReader.fetchFully(readOffset);
    //        if (found.get()) {
    //            topicReader.shutdown();
    //            inEventsAsync.complete();
    //            break;
    //        } else {
    //            try {
    //                TimeUnit.SECONDS.sleep(1);
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //            readOffset = nextOffset;
    //        }
    //    }
    }
}
