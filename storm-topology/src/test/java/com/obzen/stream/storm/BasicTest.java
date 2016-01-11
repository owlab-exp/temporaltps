package com.obzen.stream.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
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


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class BasicTest {

    public static class ExclamationBolt extends BaseRichBolt {
        private OutputCollector collector;

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void execute(Tuple tuple) {
            collector.emit(tuple, new Values(tuple.getString(0) + "!"));
            collector.ack(tuple);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word"));
        }
    }

    @Test
    public void testBasicTopologyOnLocal() {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("word", new TestWordSpout(), 1);
        builder.setBolt("first_exclaim", new ExclamationBolt(), 1).shuffleGrouping("word");
        builder.setBolt("second_exclaim", new ExclamationBolt(), 2).shuffleGrouping("first_exclaim");

        Config conf = new Config();
        conf.setDebug(true);

        //if (args != null && args.length > 0) {
         //   conf.setNumWorkers(2);
         //   // Submit to remote Storm cluster
         //   StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        // } else {
            // Start a local Storm cluster for testing
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("storm-topo-exclaim", conf, builder.createTopology());
            Utils.sleep(20000);
            cluster.killTopology("storm-topo-exclaim");
            cluster.shutdown();
        //}
    }
}
