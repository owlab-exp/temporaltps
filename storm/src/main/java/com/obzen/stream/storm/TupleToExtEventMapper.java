package com.obzen.stream.storm;

import com.obzen.common.event.ExtEvent;
import com.obzen.common.serializer.impl.ExternalEventSerializer;

import backtype.storm.tuple.Tuple;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TupleToExtEventMapper implements TupleToKafkaMapper<String, byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(TupleToExtEventMapper.class);

    private ExternalEventSerializer serializer;

    // The serializer should handle the tuple's elements in ordered way
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

        ExtEvent[] extEvents = new ExtEvent[]{new ExtEvent(System.currentTimeMillis(), tuple.getValues().toArray())};

        byte[] result = null;

        try {
            result = serializer.serializeArray(extEvents);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    
}
