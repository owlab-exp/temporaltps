package com.obzen.stream.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ExtractVenueFieldsBolt extends BaseRichBolt {
    private static Logger logger = LoggerFactory.getLogger(ExtractVenueFieldsBolt.class);

    private OutputCollector collector;

    private JSONParser parser = new JSONParser();

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
        collector = _collector;
    }
    
    @Override
    public void execute(Tuple tuple) {
        String jsonStr = tuple.getString(0);
        logger.debug(jsonStr);

        Object obj = null;
        try {
            obj = parser.parse(jsonStr);
        } catch(ParseException pe) {
            logger.error("Error: {}", pe.getMessage());
        }

        JSONObject json = (JSONObject)obj; //JSONObject => Map
        collector.emit(tuple, new Values(
                    json.get("zip"),
                    json.get("country"),
                    json.get("city"),
                    json.get("address_1"),
                    json.get("name"),
                    json.get("id"),
                    json.get("mtime")
                    ));

        collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(
                    "zip", 
                    "country", 
                    "city", 
                    "address_1", 
                    "name", 
                    "id", 
                    "mtime"
                    ));
    }
}