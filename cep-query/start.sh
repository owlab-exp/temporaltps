#!/bin/sh
TARGET_HOST=172.17.8.101
gradle run -Pargs="-c start -e ${TARGET_HOST}:8082 -n node.${TARGET_HOST}.5800 -j ./src/main/resources/meetup_event_cq.json"
