package com.obzen.stream.storm;

import com.obzen.common.serializer.FieldType;
import com.obzen.common.serializer.impl.ExternalEventSerializer;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.Nimbus.Client;
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
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Utils;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.KafkaConfig;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class SimpleMeetUpParseTopology implements Serializable {
    //private static final String[] requiredEnvVars = {
    //    "ZK_HOSTS",
    //    "KAFKA_HOSTS",
    //    "KAFKA_TOPIC_SRC",
    //    "KAFKA_TOPIC_TGT"
    //};

    //private static String zkHosts = "172.17.8.101:2181"; // For sourcing
    //private static String kafkaHosts = "172.17.8.101:9092"; //For sinking
    //private static String topic_src = "meetup_venues";
    //private static String topic_tgt = "venues_parsed";

    public StormTopology buildTopology(String zkHosts, String topic_src, String topic_tgt) {
        // Build a topology
        TopologyBuilder builder = new TopologyBuilder();
        
        // Prepare Core KafkaSpout
        BrokerHosts brokers =  new ZkHosts(zkHosts);
        SpoutConfig spoutConfig = new SpoutConfig(brokers, topic_src, "/" + topic_src, UUID.randomUUID().toString());
        spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);

        // Set KafkaSpout
        builder.setSpout("venues", kafkaSpout, 1);

        // Set bolt for lines to fields
        builder.setBolt("extractFields", new ExtractVenueFieldsBolt(), 1).shuffleGrouping("venues");

        // Kafka Sink bolt
        // Serialization for CEP consumer
        ExternalEventSerializer serializer = ExternalEventSerializer.builder()
                                    .addDataFieldType(FieldType.STRING) //zip
                                    .addDataFieldType(FieldType.STRING) //country
                                    .addDataFieldType(FieldType.STRING) //state
                                    .addDataFieldType(FieldType.STRING) // city
                                    .addDataFieldType(FieldType.STRING) //address_1
                                    .addDataFieldType(FieldType.STRING) //address_2
                                    .addDataFieldType(FieldType.STRING) //address_3
                                    .addDataFieldType(FieldType.DOUBLE) //lat
                                    .addDataFieldType(FieldType.DOUBLE) //lon
                                    .addDataFieldType(FieldType.LONG) //id
                                    .addDataFieldType(FieldType.STRING) //name
                                    .addDataFieldType(FieldType.STRING) //phone
                                    .build();
        KafkaBolt<String, byte[]> sinkBolt = new KafkaBolt<String, byte[]>()
            //.withProducerProperties(props) // not valid in version Storm 0.10.0
            .withTopicSelector(new DefaultTopicSelector(topic_tgt))
            .withTupleToKafkaMapper(new TupleToExtEventMapper(serializer));
        builder.setBolt("sinkToKafka", sinkBolt, 1).shuffleGrouping("extractFields");

        return builder.createTopology();
    }

    public static void main(String[] args) throws Exception {

        OptionParser parser = new OptionParser();
        OptionSpec<String> zkHostsOp = parser.accepts("z").withRequiredArg().ofType(String.class).describedAs("zkHosts");
        OptionSpec<String> kafkaHostsOp = parser.accepts("k").withRequiredArg().ofType(String.class).describedAs("kafkaHosts");
        OptionSpec<String> topologyNameOp = parser.accepts("n").withRequiredArg().ofType(String.class).describedAs("Topology name");
        OptionSpec<String> topicSrcOp = parser.accepts("s").withRequiredArg().ofType(String.class).describedAs("Source topic");
        OptionSpec<String> topicTgtOp = parser.accepts("t").withRequiredArg().ofType(String.class).describedAs("Target topic");

        OptionSet options = parser.parse(args);

        if(!(options.has(zkHostsOp) && options.hasArgument(zkHostsOp) &&
             options.has(kafkaHostsOp) && options.hasArgument(kafkaHostsOp) &&
             options.has(topologyNameOp) && options.hasArgument(topologyNameOp) &&
             options.has(topicSrcOp) && options.hasArgument(topicSrcOp) &&
             options.has(topicTgtOp) && options.hasArgument(topicTgtOp)
             )) {
            parser.printHelpOn(System.out);
            System.exit(0);
             }

        String zkHosts = options.valueOf(zkHostsOp);
        String kafkaHosts = options.valueOf(kafkaHostsOp);
        String topologyName = options.valueOf(topologyNameOp);
        String topic_src = options.valueOf(topicSrcOp);
        String topic_tgt = options.valueOf(topicTgtOp);

        SimpleMeetUpParseTopology topologyProvider = new SimpleMeetUpParseTopology();
        StormTopology topologyToDeploy = topologyProvider.buildTopology(zkHosts, topic_src, topic_tgt);


        Config conf = new Config();
        //Kafka Producer config
        Properties props = new Properties();
        //props.put("bootstrap.servers.", kafkaHosts);
        props.put("metadata.broker.list", kafkaHosts); //Weird!
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, props); // version 0.10.0

        conf.setNumWorkers(2);
        //conf.setDebug(true);
        // Submit to remote Storm cluster
        StormSubmitter.submitTopology(topologyName, conf, topologyToDeploy);
    }
}
