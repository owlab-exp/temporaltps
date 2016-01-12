package com.obzen.stream.reader;

import com.obzen.common.serializer.impl.ExternalEventSerializer;
import com.obzen.common.serializer.FieldType;
import com.obzen.common.event.ExtEvent;
import com.obzen.common.kafka.KafkaTopicDisruptReader;
import com.obzen.common.kafka.KafkaTopicReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class EndKafkaReader {

    public static void main(String[] args) {
        // Prepare event serializers in ordered way
        ExternalEventSerializer serializer = ExternalEventSerializer.builder()
                .addDataFieldType(FieldType.STRING)
                .addDataFieldType(FieldType.STRING)
                .addDataFieldType(FieldType.LONG)
                .build();
        
        // Consume events
        String topic = "country_city";
        Properties props = new Properties();
        props.setProperty("zk.urls", "172.17.8.101:2181");
        props.setProperty("kafka.urls", "172.17.8.101:9092");
        props.setProperty("kafka.group.id", "test-consumer-01");
        KafkaTopicReader topicReader = new KafkaTopicDisruptReader(topic, props) {
            @Override
            public void processEventsBytes(long currentOffset, byte[] bytes) {
                try {
                    ExtEvent[] events = serializer.deserializeArray(bytes);
                    System.out.println("RECV: " + Arrays.deepToString(events));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        long readOffset = topicReader.findReadOffset();
        System.out.println("Offset read: " + readOffset);
        long nextOffset = -1L;
        while (true) {
            nextOffset = topicReader.fetchFully(readOffset);
            readOffset = nextOffset;
        }
    }
}

/*
import com.obzen.common.util.ValueWaiter;
import rx.Observable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

    private static NodeServer server;
    private static Vertx vertx;
    private static String deploymentID;
    final float sampleValue = 240;

    @BeforeClass
    public static void startEventProcess(TestContext testCtx) throws Exception {
        // Start vertx
        CountDownLatch vertxStartupLatch = new CountDownLatch(1);

        String nodeHost = "localhost";
        int nodePort = 5801;

        server = new NodeServer("hz", nodeHost, nodePort);
        server.disableVertxMetrics();

        Observable<Vertx> observableServer = server.observableStart();
        observableServer.subscribe(
                createdVertx -> {
                    testCtx.assertNotNull(createdVertx);
                    vertx = createdVertx;
                    System.out.println("DEV MSG: Vertx created.");
                    vertxStartupLatch.countDown();
                },
                exception -> {
                    exception.printStackTrace();
                    testCtx.fail();
                });

        vertxStartupLatch.await();

        System.out.println("DEV MSG: Deploying component.");
        String fileURIString = EventProcessorKafkaTopicFeedingTest.class.getClassLoader().getResource("SampleEventProcessorKafkaTopicConfig.json").toURI().toString();
        System.out.println("fileURIString: " + fileURIString);
        EventProcessorConfig eventProcessorConfig = JsonUtil.fromJsonFile(fileURIString.split(":")[1], EventProcessorConfig.class);
        JsonObject config = JsonUtil.toJsonObject(eventProcessorConfig);
        System.out.println(config.encodePrettily());

        StartupOptions startupOptions = new StartupOptions()
                .setWorker(true)
                .setConfig(config);

        Observable<String> observableDeployment = vertx.deployVerticleObservable("service:ocep.EventProcessor", startupOptions);

        CountDownLatch deploymentLatch = new CountDownLatch(1);

        observableDeployment.subscribe(
                id -> {
                    // check if id is not null
                    testCtx.assertNotNull(id);

                    System.out.println("Deployment succeeded with deploymentID: " + id);
                    deploymentID = id;

                    deploymentLatch.countDown();
                },
                throwable -> {
                    throw new RuntimeException(throwable);
                });

        deploymentLatch.await();
    }

    @AfterClass
    public static void cleanup(TestContext testCtx) throws Exception {
        System.out.println("Now cleaning resources up");
        //vertx.undeploy(deploymentID, testCtx.asyncAssertSuccess());
        //TODO why the above line causes "already undeployed exception" by the following code?
        CountDownLatch latch = new CountDownLatch(1);
        //vertx.close(testCtx.asyncAssertSuccess());
        vertx.close(closed -> latch.countDown());

        latch.await();
    }

    private ExtEvent[] createEvents() {
        return new ExtEvent[]{
                //new ExtEvent(System.currentTimeMillis(), new Object[]{"IBM", Float.valueOf(sampleValue), "DUMMY"})
                new ExtEvent(System.currentTimeMillis(), new Object[]{null, Float.valueOf(sampleValue), "DUMMY"})
        };
    }

    @Test
    public void eventFeedTest(TestContext testCtx) throws Exception {
        final int feedCount = 1000;

        ExternalEventSerializer inSiddhiEventSerializer = ExternalEventSerializer.builder()
                .addDataFieldType(FieldType.STRING)
                .addDataFieldType(FieldType.DOUBLE)
                //.addDataFieldType(FieldType.FLOAT)
                .addDataFieldType(FieldType.OBJECT)
                .build();


        String groupId = "ocep-kafka-group";
        String clientId = "ocep-kafka-client";
        //String brokerHost = "172.17.8.101";
        //int brokerPort = 9092;


        // Produce events
        String outTopic = "sampleInName0";
        Properties p_props = new Properties();
        p_props.put("bootstrap.servers", "172.17.8.101:9092");
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
        Async inEventsAsync = testCtx.async();
        final AtomicBoolean found = new AtomicBoolean(false);
        final ValueWaiter<Double> valueReceiveWaiter = new ValueWaiter<Double>(Double.valueOf(feedCount * sampleValue)) {
            @Override
            public void whenFound() {
                found.set(true);
            }
        };

        // Consume events
        String inTopic = "sampleOutName0";
        KafkaTopicReader topicReader = new KafkaTopicDisruptReader(inTopic) {
            @Override
            public void processEventsBytes(long currentOffset, byte[] bytes) {
                try {
                    ExtEvent[] events = inSiddhiEventSerializer.deserializeArray(bytes);
                    System.out.println("RECV: " + Arrays.deepToString(events));
                    for (int i = 0; i < events.length; i++) {
                        valueReceiveWaiter.checkValue((Double) events[i].getData()[1]);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        long readOffset = topicReader.findReadOffset();
        System.out.println("Offset read: " + readOffset);
        long nextOffset = -1L;
        while (true) {
            nextOffset = topicReader.fetchFully(readOffset);
            if (found.get()) {
                topicReader.shutdown();
                inEventsAsync.complete();
                break;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                readOffset = nextOffset;
            }
        }
    }
}
*/
