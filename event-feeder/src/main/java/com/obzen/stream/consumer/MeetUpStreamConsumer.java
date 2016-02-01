package com.obzen.stream.consumer;

import com.owlab.restful.weakrest.WeakRestClient;
import com.owlab.restful.weakrest.StreamReader;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import rx.Observable;
import rx.Subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

public class MeetUpStreamConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MeetUpStreamConsumer.class);

    private static final String[] requiredEnvVars = {
                "MEETUP_ENDPOINT",
                "KAFKA_BOOTSTRAP_SERVERS",
                "KAFKA_TOPIC",
                "KAFKA_CLIENT_ID"
    };

    private static final Map<String, String> env = System.getenv();

    public static void main(String[] args) {

        checkEnv();

        //String endPoint = "http://stream.meetup.com/2/open_venues?trickle";
        String endPoint = env.get(requiredEnvVars[0]);
        String bootStrapServers = env.get(requiredEnvVars[1]);
        String topic = env.get(requiredEnvVars[2]);
        String clientId = env.get(requiredEnvVars[3]);

        //Set minimum Kafka producer properties with additional default values
        Properties kafkaProducerProps = new Properties();
        kafkaProducerProps.put("bootstrap.servers", bootStrapServers);
        kafkaProducerProps.put("acks", "1");
        //kafkaProducerProps.put("buffer.memory", "33554432");
        //kafkaProducerProps.put("batch.size", "16384");
        kafkaProducerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducerProps.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        kafkaProducerProps.put("client.id", clientId);

        //Prepare KafkaProducer
        KafkaProducer<Void, byte[]> producer = new KafkaProducer<>(kafkaProducerProps);

        //// Synchronous example
        //try {
        //    WeakRestClient.get(endPoint)
        //        .execute(line -> {
        //            logger.info(line);
        //        });
        //} catch(Exception e) {
        //    e.printStackTrace();
        //}

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                try {
                    if(!observer.isUnsubscribed()) {
                        WeakRestClient restClient = WeakRestClient.get(endPoint)
                                                                  .setConnectionTimeout(30000)
                                                                  .setSocketTimeout(30000);
                        
                        restClient.execute(line -> observer.onNext(line));
                    }
                    //there is no chace of observer.onCompleted()
                } catch(Exception e) {
                    observer.onError(e);
                }
            }
        })
        .doOnNext(line -> System.out.println("RAW: " + line))
        //Skip blank or blank json
        .filter(line -> !(line.equalsIgnoreCase("{}") || line.trim().equals("")))
        //Add timestamp
        .map(line -> line.replace("{", "{\"timestamp\":" + System.currentTimeMillis() + ","))
        //.doOnNext(line -> System.out.println("Timestamped: " + line))
        .map(line -> new ProducerRecord<Void, byte[]>(topic, line.getBytes()))
        .flatMap(record -> Observable.from(producer.send(record)))
        .subscribe(new Subscriber<RecordMetadata>() {
            @Override
            public void onNext(RecordMetadata metadata) {
                logger.info("Kafka Result - topic: {}, partition: {}, offset:{}", metadata.topic(), metadata.partition(), metadata.offset());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error: " + t.getMessage());
                producer.close();
                throw new RuntimeException(t);
            }
            
            @Override
            public void onCompleted() {
                //Can not get here in anyway
                producer.close();
            }
        })
        ;
    }

    public static void checkEnv() {
        if(Stream.<String>of(requiredEnvVars)
            .peek(key -> logger.info("Required Env. Key {}={}", key, env.get(key)))
            .filter(key -> !env.containsKey(key))
            .findAny()
            .isPresent()) {
                throw new RuntimeException("One or more required environment variables are missing");
            }
    }
}
