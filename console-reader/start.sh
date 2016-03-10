#!/bin/sh
TARGET_HOST=172.17.8.101
gradle run -Pargs="-k ${TARGET_HOST}:9092 -t meetup_venue_out -g test-group"
