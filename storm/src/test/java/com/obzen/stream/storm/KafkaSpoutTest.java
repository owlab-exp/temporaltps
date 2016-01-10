package com.obzen.stream.storm;

import com.obzen.common.serializer.FieldType;
import com.obzen.common.serializer.impl.ExternalEventSerializer;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.testing.TestWordSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.KafkaConfig;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.selector.DefaultTopicSelector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class KafkaSpoutTest {
    private static String zkHosts = "192.168.0.201:2181";
    private static String kafkaHosts = "192.168.0.201:9092";
    private static String src_topic = "meetup_venues";
    private static String sink_topic = "venues_parsed";

    @Test
    public void testKafkaSpout() {
        // Prepare Core KafkaSpout
        BrokerHosts brokers =  new ZkHosts(zkHosts);
        //SpoutConfig spoutConfig = new SpoutConfig(brokers, src_topic, "/" + src_topic, UUID.randomUUID().toString());
        SpoutConfig spoutConfig = new SpoutConfig(brokers, src_topic, "/" + src_topic, "example_client");
        //KafkaConfig spoutConfig = new KafkaConfig(brokers, src_topic, "example_client");
        spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);

        // Build a topology
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("venues", kafkaSpout, 1);
        builder.setBolt("extractFields", new ExtractVenueFieldsBolt(), 3).shuffleGrouping("venues");

        //Kafka Producer config
        Properties props = new Properties();
        props.put("bootstrap.servers.", kafkaHosts);
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        
        //
        ExternalEventSerializer outTupleSerializer = ExternalEventSerializer.builder()
            .addDataFieldType(FieldType.STRING) //zip
            .addDataFieldType(FieldType.STRING) //country
            .addDataFieldType(FieldType.STRING) //...
            .addDataFieldType(FieldType.STRING)
            .addDataFieldType(FieldType.STRING)
            .addDataFieldType(FieldType.STRING)
            .addDataFieldType(FieldType.LONG) //mtime
            .build();

        // Kafka Sink bolt
        KafkaBolt sinkBolt = new KafkaBolt()
            //.withProducerProperties(props) // after version 0.10.0
            .withTopicSelector(new DefaultTopicSelector(sink_topic))
            .withTupleToKafkaMapper(new TupleToExtEventMapper(outTupleSerializer));
        builder.setBolt("sinkToKafka", sinkBolt, 1).shuffleGrouping("extractFields");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, props); // version 0.10.0

        //if (args != null && args.length > 0) {
        //   conf.setNumWorkers(2);
        //   // Submit to remote Storm cluster
        //   StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        // } else {
            // Start a local Storm cluster for testing
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("meetup-top", conf, builder.createTopology());
            Utils.sleep(20000);
            cluster.killTopology("meetup-top");
            cluster.shutdown();
        //}
    }
}
