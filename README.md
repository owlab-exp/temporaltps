# Event stream processing with Storm/Kafka/obzenCEP

## event-feeder
MeetUp Stream API들 중 하나(http://stream.meetup.com/2/open_venues?trickle)로부터 이벤트를 전송받아, 한 라인씩 Kafka (topic: meetup_venues)로 재전송.
 아래와 같은 명력으로 커맨드라인에서 MeetUp의 실제 >이벤트들을 확인할 수 있음:  
> ```curl -i http://stream.meetup.com/2/open_venues?trickle```\

이 프로그램은 docker로 패키지가 되어 있으므로, 서브 디렉토리인 docker-build 안에 있는 meetup-venues.service를 이용함. 즉 fleet으로 원하는 CoreOS 시스템에 submit/load/start 하면 됨. 

## storm-topology
Kafka에서 이벤트 데이터(string)를 읽어들여 필드들(zip, country, city, ...)로 구분하는 작업을 담당.
CEP가 읽어들일 수 있는 형태의 byte array로 convert하여 Kafka (topic: venue_parsed)_에 전송. 
샘플 토폴로지는 하나의 Spout와 두 개의 Bolt로 이루어짐: 
- Kafka Spout: read from Kafka
- ExtractVenueFieldsBolt: parse lines to JSON object and extrace fields - 'zip', 'country', ... 
- Kafka Bolt: read field values and serialize the values for CEP, and send to Kafka
로컬 컴퓨터에서 실행하려면, 필요하다면 ExampleTopologyProvider.java 와 ExampleTopologyTest를 편집한 후, 다음 명령을 이용: 

> ```gradle cleanTest test```

원격에서 실행중인 Storm에 토폴로지를 deploy하기 위해서는, 

> ```gradle shadowJar```

> ```storm jar ./build/libs/storm-topology-0.8-SNAPSHOT-all.jar com.obzen.stream.storm.ExampleTopologyProvider```

원격 Storm클러스터에 deploy된 토폴로지들의 확인

> ```storm list``` 

특정 토폴로지를 중단
> ```storm kill <topology name>```

## cep-query
Storm 의 처리결과를 읽어들여, Siddhi CEP 쿼리를 수행

Start a component definition
```gradle run -Pargs="-c start -e 192.168.10.82:8082 -n node.192.168.10.83.5800 -j ./src/main/resources/meetup_event_cq.json"```
Stop a component definition
```gradle run -Pargs="-c stop -e 192.168.10.82:8082 -n node.192.168.10.83.5800 -j ./src/main/resources/meetup_event_cq.json"```

## end-kafka-reader
CEP의 처리결과를 조회하기 위한 간단한 Kafka Reader
