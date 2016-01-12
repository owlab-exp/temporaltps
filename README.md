# Event stream processing with Storm/Kafka/obzenCEP

## event-feeder
A MeetUp Stream API (http://stream.meetup.com/2/open_venues?trickle)
Try:
```curl -i http://stream.meetup.com/2/open_venues?trickle``

## storm-topology
### An example topology:
- Kafka Spout: read from Kafka
- ExtractVenueFieldsBolt: parse lines to JSON object and extrace fields - 'zip', 'country', ...
- Kafka Bolt: read field values and serialize the values for CEP, and send to Kafka
### Usuage


## cep-query
An example continuous query
- src/main/resources/meetup_event_cq.json
- 

