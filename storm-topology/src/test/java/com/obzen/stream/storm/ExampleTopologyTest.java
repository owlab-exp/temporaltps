package com.obzen.stream.storm;

import com.obzen.common.serializer.FieldType;
import com.obzen.common.serializer.impl.ExternalEventSerializer;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
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
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class ExampleTopologyTest implements Serializable {
    private static String zkHosts = "172.17.8.101:2181"; // For sourcing
    private static String kafkaHosts = "172.17.8.101:9092"; //For sinking
    private static String src_topic = "meetup_venues";
    private static String sink_topic = "venues_parsed";

    @Test 
    public void testTopology() {

        ExampleTopologyProvider topologyProvider = new ExampleTopologyProvider();
        StormTopology  topology = topologyProvider.buildTopology();

        Config conf = new Config();
        conf.setDebug(true);
        //Kafka Producer config
        Properties props = new Properties();
        //props.put("bootstrap.servers.", kafkaHosts);
        props.put("metadata.broker.list", kafkaHosts); //Weird!
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, props); // version 0.10.0

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("meetup-topology", conf, topology);
        Utils.sleep(10000);
        cluster.killTopology("meetup-topology");
        cluster.shutdown();
    }

    /**
     * For easy understanding
    @Test
    public void testTopologyOld() {
        // Prepare Core KafkaSpout
        BrokerHosts brokers =  new ZkHosts(zkHosts);
        SpoutConfig spoutConfig = new SpoutConfig(brokers, src_topic, "/" + src_topic, UUID.randomUUID().toString());
        spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);

        // Build a topology
        TopologyBuilder builder = new TopologyBuilder();
        // Set spout
        builder.setSpout("venues", kafkaSpout, 1);

        // Set bolt
        builder.setBolt("extractFields", new ExtractVenueFieldsBolt(), 3).shuffleGrouping("venues");

        // Kafka Sink bolt
        // Serialization for CEP consumer
        ExternalEventSerializer serializer = ExternalEventSerializer.builder()
                                    .addDataFieldType(FieldType.STRING) //zip
                                    .addDataFieldType(FieldType.STRING) //country
                                    .addDataFieldType(FieldType.STRING) //...
                                    .addDataFieldType(FieldType.STRING)
                                    .addDataFieldType(FieldType.STRING)
                                    .addDataFieldType(FieldType.LONG) //id
                                    .addDataFieldType(FieldType.LONG) //mtime
                                    .build();
        KafkaBolt sinkBolt = new KafkaBolt()
            //.withProducerProperties(props) // not valid in version Storm 0.10.0
            .withTopicSelector(new DefaultTopicSelector(sink_topic))
            .withTupleToKafkaMapper(new TupleToExtEventMapper(serializer));
        builder.setBolt("sinkToKafka", sinkBolt, 1).shuffleGrouping("extractFields");

        Config conf = new Config();
        conf.setDebug(true);
        //Kafka Producer config
        Properties props = new Properties();
        //props.put("bootstrap.servers.", kafkaHosts);
        props.put("metadata.broker.list", kafkaHosts); //Weird!
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
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
    */
}
