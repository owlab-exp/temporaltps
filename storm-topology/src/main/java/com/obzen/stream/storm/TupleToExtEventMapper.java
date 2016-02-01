package com.obzen.stream.storm;

import com.obzen.common.event.ExtEvent;
import com.obzen.common.serializer.FieldType;
import com.obzen.common.serializer.impl.ExternalEventSerializer;

import backtype.storm.tuple.Tuple;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TupleToExtEventMapper implements TupleToKafkaMapper<String, byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(TupleToExtEventMapper.class);

    protected ExternalEventSerializer serializer;

    public TupleToExtEventMapper(ExternalEventSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public String getKeyFromTuple(Tuple tuple) {
        //Nothing to do
        return null;
    }

    @Override
    public byte[] getMessageFromTuple(Tuple tuple) {

        if(!tuple.contains("timestamp")) {
            return null;
        }

        Long timestamp = tuple.getLongByField("timestamp");
        List<Object> values = tuple.getValues();
        //Remove the timestamp to handle differently
        values.remove(timestamp);

        ExtEvent[] extEvents = new ExtEvent[]{new ExtEvent(timestamp, values.toArray())};

        byte[] result = null;

        try {
            result = serializer.serializeArray(extEvents);
            logger.info("Serialized Size: {}", result.length);
        } catch(Exception e) {
            logger.error("Exception while serializing a tuple: {}", e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
    
}
