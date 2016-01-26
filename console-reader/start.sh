#!/bin/sh
TARGET_HOST=172.17.8.101
gradle run -Pargs="-z ${TARGET_HOST}:2181 -k ${TARGET_HOST}:9092 -t meetup-venue-out -g test-group"
