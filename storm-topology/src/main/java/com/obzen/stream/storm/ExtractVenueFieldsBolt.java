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

    private JSONParser parser; //= new JSONParser();

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
        collector = _collector;
        parser = new JSONParser();
    }
    
    @Override
    public void execute(Tuple tuple) {
        String jsonStr = null;
        try {
            jsonStr = tuple.getString(0);
            logger.info("tuple:0: {}" + jsonStr);

            Object obj = parser.parse(jsonStr);

            JSONObject json = (JSONObject)obj; //JSONObject => Map
            //For venue event elements, please refer to http://www.meetup.com/meetup_api/docs/stream/2/open_venues/#http
            collector.emit(tuple, new Values(
                        (Long) json.get("timestamp"),
                        (String) json.get("zip"),
                        (String) json.get("country"),
                        (String) json.get("state"),
                        (String) json.get("city"),
                        (String) json.get("address_1"),
                        (String) json.get("address_2"),
                        (String) json.get("address_3"),
                        (Double) json.get("lat"),
                        (Double) json.get("lon"),
                        (Long) json.get("id"),
                        (String) json.get("name"),
                        (String) json.get("phone")
                        ));
            collector.ack(tuple);
        } catch(Exception e) {
            logger.error("Error while parsing: {}", jsonStr);
            collector.reportError(e);
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(
                    "timestamp", 
                    "zip", 
                    "country", 
                    "state", 
                    "city", 
                    "address_1", 
                    "address_2", 
                    "address_3", 
                    "lat", 
                    "lon", 
                    "id", 
                    "name", 
                    "phone" 
                    ));
    }
}
