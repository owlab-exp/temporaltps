#!/bin/sh
TARGET_HOST=172.17.8.101
TOPOLOGY_NAME=meetup_parse_topology
SRC_TOPIC=meetup_venue_lines
TGT_TOPIC=meetup_venue_events
storm jar ./build/libs/storm-topology-0.8-SNAPSHOT-all.jar com.obzen.stream.storm.SimpleMeetUpParseTopology -z ${TARGET_HOST}:2181 -k ${TARGET_HOST}:9092 -n ${TOPOLOGY_NAME} -s ${SRC_TOPIC} -t ${TGT_TOPIC}
