# A stream processing example with Storm/Kafka/obzenCEP

## Overview
이 프로젝트는 Apache Kafka, Apache Storm 그리고 obzen CEP를 함께 이용하여 어떻게 실시간 이벤트를 처리하는지를 보여주고자 작성된 간단한 샘플로서, 이벤트 처리 흐름을 간단히 살펴보면 다음과 같음.

| Sub project | Engine | Role | Source | Target |
|---|---|---|---|---|
|event-feeder|  | MeetUp의 Stream API로부터 venue 스트림을 받아, 각 행을 한 레코드로 만들어, Kafka의 topic에 전달. | MeetUP: <br> [open_venues](http://stream.meetup.com/2/open_venues?tickle) | Kafka Topic: <br> meetup_venue_lines|
| storm-topology | Apache Storm | **event-feeder**가 생성한 레코드들을 각각 Json Object로 변환하고, 이 Json Object로부터 특정 필드들로 이루어진 tuple들을 생성. 이 tuple들을 CEP가 읽어들일 수 있는 형태로 변환하여 Kafka의 또 다른 topic에 전달. | Kafka Topic:<br> meetup_venue_lines | Kafka Topic:<br> meetup_venue_events |
|cep-query | obzenCEP |**storm-topology**에 의해 생성된 이벤트들을 읽어들여, CEP Query를 적용하고 결과 스트림을 또 다른 Kafka Topic에 전달. | Kafka Topic:<br> meetup_venue_events | Kafka Topic:<br> meetup_venue_out |
| console-reader | | **cep-query**로부터 산출된 이벤트들을 읽여들여 console에서 확인할 수 있도록 한 dummy reader | Kafka Topic:<br> meetup_venue_out | Console |

다음의 각 섹션은 각 서브 프로젝트들에 대한 보다 상세한 설명.

## Before proceed
이하 각 서프 프로젝트들에 대한 설명은 Linux에서 테스트할 때를 기준으로 하고 있으며,
Vagrant를 이용하여 CoreOS VM이 실행되고 있고, 그 위에 Zookeeper, Kafka, Storm, Cassandra, 그리고 obzenCEP가 docker container들로서 운영되고 있을 때를 전제로 하고 있다.
또한, 이 CoreOS 외부에서 fleetctl로 서비스들을 컨트롤하기 위해서는 다음의 명령이 미리 실행되어야 한다.

> export FLEETCTL_TUNNEL=172.17.8.101:22
> ssh-add ~/.vagrant.d/insecure_private_key

## event-feeder
### Stream source
MeetUp Stream API들 중 하나(http://stream.meetup.com/2/open_venues?trickle)로부터 이벤트를 전송받아, 한 라인씩 Kafka Topic (**meetup_venue_lines**)으로 전송.
스트림 API의 출력은 아래와 같은 명령으로 커맨드라인에서도 확인할 수 있음 (Linux의 경우):  
> curl -i http://stream.meetup.com/2/open_venues?trickle

또는 웹 브라우저에 위 URL을 입력하여 화면으로도 확인 가능.
### Main executable
주 실행 프로그램은 **event-feeder/build.gradle**을 열어 확인할 수 있음.

    mainClassName = 'com.obzen.stream.consumer.MeetUpStreamConsumer'

따라서 이 메인 프로그램의 소스는 event-feeder/src/main/java/com/obzen/stream/consumer/MeetUpStreamConsumer.java라는 것을 알 수 있다.

### Run in Gradle
테스트의 용도로, Gradle에서 메인 프로그램을 실행하기 위해서는 이 프로그램이 요구하는 환경변수를 셋팅하여야 하는데,  **event-feeder/build.gradle**의 다음 섹션에서 프로그램의 실행에 필요한 환경변수들을 확인할 수 있다.

    run {
        environment "MEETUP_ENDPOINT", "http://stream.meetup.com/2/open_venues?trickle"
        environment "KAFKA_BOOTSTRAP_SERVERS", "172.17.8.101:9092" 
        environment "KAFKA_TOPIC", "meetup_venue_lines"
        environment "KAFKA_CLIENT_ID", "meetup_client"
    }
필요하다면 **event-feeder/build.gradle**을 편집하여 위 설정들을 변경한 후, 다음의 명령으로 실행해볼 수 있다.
>cd event-feeder
>gradle run

### Dockerize & push to a private docker registry
상기의 프로그램을 CoreOS 클러스터에서 운영하기 위해서는 docker image를 생성하는 것이 선행되어야 하므로 다음의 명령들을 이용한다.
event-feeder 디렉토리에서;

> gradle distTar
> cd docker-build
> ./repub.sh

- ```gradle distTar```는 docker image를 만들 때 사용할 프로그램 및 연관 라이브러리의 tar archive를 생성하기 위한 것이며, 
- ```docker-build```는 Docker 이미지를 작성하기 위한 스크립트(```Dockerfile```) 를 포함하고 있다. 
- ```repub.sh```는 docker 이미지를 작성하고, registry에 push하기 위하 스크립트이다.

### Run in a CoreOS
Docker registry에 있는 이미지를 CoreOS에서 실행하기 위해서는 **fleetctl**을 이용한다. **fleetctl**은 서비스가 정의된 unit file을 필요로 하며, ```docker-build/meetup-venues.service```가 그것이다. 이 파일을 열어보면 위에서 build.gradle에 정의하였던 시스템 환경변수들을 docker container에 약간 다른 방식으로 셋팅하고 있는 것을 알 수 있다.

    -e MEETUP_ENDPOINT="http://stream.meetup.com/2/open_venues?trickle" \
    -e KAFKA_BOOTSTRAP_SERVERS=$KAFKA_URLS \
    -e KAFKA_TOPIC=meetup_venue_lines \
    -e KAFKA_CLIENT_ID=meetup_client \

(필요하다면, 이 파일을 편집하여 셋팅을 바꾸고) 아래와 같은 fleetctl 명령으로 CoreOS에서 실행한다. 
docker-build 디렉토리 안에서;
> fleetctl submit meetup-venues
> fleetctl load meetup-venues
> fleetctl start meetup-venues

그리고 ```fleetctl journal -f meetup-venues``` 명령으로 실행상태를 확인해볼 수 있다.
이 서비스를 중단하기 위해서는,
> fleetctl stop meetup-venues

## storm-topology
Kafka에서 이벤트 데이터(string)를 읽어들여 필드들(zip, country, city, ...)로 구분하는 작업을 담당.
CEP가 읽어들일 수 있는 형태의 byte array로 convert하여 Kafka (topic: venue_parsed)_에 전송. 
샘플 토폴로지는 하나의 Spout와 두 개의 Bolt로 이루어짐: 
- Kafka Spout: read from Kafka
- ExtractVenueFieldsBolt: parse lines to JSON object and extract fields - 'zip', 'country', ... 
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
