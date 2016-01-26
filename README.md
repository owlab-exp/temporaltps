# A stream processing example with Storm/Kafka/obzenCEP

## Overview
이 프로젝트는 Apache Kafka, Apache Storm 그리고 obzen CEP를 함께 이용하여 실시간 스트림 데이터를  처리하는 간단한 샘플이다,<br>
이벤트 처리 흐름을 간단히 살펴보면 다음과 같다.

| Sub project | Engine | Role | Source | Target |
|---|---|---|---|---|
|event-feeder|  | MeetUp.com의 Stream API로부터 venue 스트림을 받아, 각 행을 한 레코드로 만들어, Kafka의 topic에 전달. | MeetUP: <br> [open_venues](http://stream.meetup.com/2/open_venues?tickle) | Kafka Topic: <br> meetup_venue_lines|
| storm-topology | Apache Storm | **event-feeder**가 생성한 레코드들을 각각 Json Object로 변환하고, 이 Json Object로부터 특정 필드들로 이루어진 tuple들을 생성. 이 tuple들을 CEP가 읽어들일 수 있는 형태로 변환하여 Kafka의 또 다른 topic에 전달. | Kafka Topic:<br> meetup_venue_lines | Kafka Topic:<br> meetup_venue_events |
|cep-query | obzenCEP |**storm-topology**에 의해 생성된 이벤트들을 읽어들여, CEP Query에 전달하고 결과 스트림을 또 다른 Kafka Topic에 전달. | Kafka Topic:<br> meetup_venue_events | Kafka Topic:<br> meetup_venue_out |
| console-reader | | **cep-query**로부터 산출된 이벤트들을 읽여들여 console에서 확인할 수 있도록 한 dummy reader | Kafka Topic:<br> meetup_venue_out | Console |

이하 각 섹션은 각 서브 프로젝트들에 대한 보다 상세한 설명이다.

## Before proceed
이하 각 서프 프로젝트들에 대한 설명은 Linux에서 테스트할 때를 기준으로 하고 있으며,

* Vagrant를 이용하여 CoreOS VM이 실행되고 있고, 그 IP는 172.17.8.101이다.
* CoreOS VM에 Zookeeper, Kafka, Storm, Cassandra, 그리고 obzenCEP가 docker container들로서 실행되고 있다.
* 로컬 시스템에 storm이 설치되어 있다.
* 호스트 obzen-reg 에 대한 IP Address가 /etc/hosts에 명시되어 있고, 그 호스트에서 docker registry가 운영되고 있어야 한다.

이 CoreOS VM 외부에서 fleetctl로 서비스들을 컨트롤하기 위해서는 다음의 명령이 미리 실행되어야 한다.

> export FLEETCTL_TUNNEL=172.17.8.101:22

> ssh-add ~/.vagrant.d/insecure_private_key


## event-feeder
-----------------------------------------------
### Stream source
MeetUp Stream API들 중 하나(http://stream.meetup.com/2/open_venues?trickle)로부터 이벤트를 전송받아, 한 라인씩 Kafka Topic (**meetup_venue_lines**)으로 전송.
스트림 API의 출력은 아래와 같은 명령으로 커맨드라인에서도 확인할 수 있다 (Linux의 경우):  

> curl -i http://stream.meetup.com/2/open_venues?trickle

또는 웹 브라우저에 위 URL을 입력하여 화면으로도 확인할 수 있을 것이다.

### Main executable
주 실행 프로그램은 **event-feeder/build.gradle**을 열어 ```mainClassName```을 확인하면 알 수 있다.

    mainClassName = 'com.obzen.stream.consumer.MeetUpStreamConsumer'

이 프로그램의 소스는 event-feeder/src/main/java/com/obzen/stream/consumer/MeetUpStreamConsumer.java라는 것도 또한 알 수 있다.<br>
소스를 살펴보면 프로그램 실행에 필요한 파라미터들을 시스템 환경변수에서 전달받는 것을 알 수 있다.

    String endPoint = env.get(requiredEnvVars[0]);
    String bootStrapServers = env.get(requiredEnvVars[1]);
    String topic = env.get(requiredEnvVars[2]);
    String clientId = env.get(requiredEnvVars[3]);

이와 같이 시스템 환경변수로부터 전달 받는 이유는, Docker로 감쌌을 때의 편의를 위해서이다.

### Run in Gradle
Gradle에서 메인 프로그램을 실행하기 위해서는 이 위의 환경변수를 셋팅하여야 하는데,  **event-feeder/build.gradle**의 다음 섹션에서 프로그램의 실행에 필요한 환경변수들을 확인할 수 있다.

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
상기의 프로그램을 CoreOS VM에서 운영하기 위해서는 docker image를 생성하는 것이 선행되어야 하므로 다음의 명령들을 이용한다.<br>
event-feeder 디렉토리에서;

> gradle distTar

> cd docker-build

> ./repub.sh

- ```gradle distTar```는 docker image를 만들 때 사용할 프로그램 및 연관 라이브러리의 tar archive를 생성하기 위한 것이며, 
- ```docker-build```는 Docker 이미지를 작성하기 위한 스크립트(```Dockerfile```) 를 포함하고 있다. 
- ```repub.sh```는 docker 이미지를 작성하고, obzen-reg registry에 push하기 위한 간단한 스크립트이다.

### Run in a CoreOS
Docker registry에 있는 이미지를 CoreOS에서 실행하기 위해서는 **fleetctl**을 이용한다.<br> 
**fleetctl**은 서비스가 정의된 unit file을 필요로 하는데, ```docker-build/meetup-venues.service```가 그것이다.<br> 
이 파일을 열어보면 위에서 build.gradle에 정의하였던 시스템 환경변수들을 docker container에 약간 다른 방식으로 셋팅하고 있는 것을 알 수 있다.

    -e MEETUP_ENDPOINT="http://stream.meetup.com/2/open_venues?trickle" \
    -e KAFKA_BOOTSTRAP_SERVERS=$KAFKA_URLS \
    -e KAFKA_TOPIC=meetup_venue_lines \
    -e KAFKA_CLIENT_ID=meetup_client \

(필요하다면, 이 파일을 편집하여 셋팅을 바꾸고) 아래와 같은 fleetctl 명령으로 CoreOS에서 실행한다. 
docker-build 디렉토리 안에서;

> fleetctl submit meetup-venues

> fleetctl load meetup-venues

> fleetctl start meetup-venues

그리고 ```fleetctl journal -f meetup-venues``` 명령으로 실행상태를 확인해볼 수 있다.<br>
이 서비스를 중단하기 위해서는 아래의 fleetctl 명령을 이용한다.

> fleetctl stop meetup-venues

## storm-topology
----------------------------
### Sample Topology
이 서브 프로젝트의 주요 프로그램 소스는 **SimpleMeetUpParseTopology.java** 이다.
이 소스를 열어보면,
 
* **KafkaSpout**: Kafka의 특정 topic으로부터 데이터를 읽어들이는 Spout
* **ExtractVenueFieldsBolt**: ***KafkaSpout***의 출력 데이터를 JsonObject로 바꾸어 **zip, country, city, address_1, name, id, mtime**의 필드 값을 추출하고, 이를 Tuple의 형태로 출력하는 Bolt
* **KafkaBolt**: ***ExtractVenueFieldsBolt***의 출력 tuple들을 CEP에서 Serializer를 이용하여, 또 다른 Kafka topic에 write하는 Bolt

로 구성되어 있는 것을 알 수 있다.

### Test in Gradle
이 SimpleMeetUpParseTopology를 로컬에서 (즉, 172.17.8.101 등 VM을 포함한 원격 시스템이 아닌) 테스트하기 위한 프로그램의 소스가 SimpleMeetUpParseTopologyTest.java 이다. 로컬에서 Storm을 실행한다고 하더라도 Zookeeper와 Kafka는 원격 시스템의 것을 사용한다. 이 테스트 프로그램 소스를 열어보면 아래와 같이 CoreOS VM에서 실행 중인 Zookeeper와 Kafka를 바라보고 있는 것을 알 수 있다.

    private static String zkHosts = "172.17.8.101:2181"; // For sourcing
    private static String kafkaHosts = "172.17.8.101:9092"; //For sinking
    private static String topic_src = "meetup_venue_lines";
    private static String topic_tgt = "meetup_venue_events";

만일, 다른 설정이 필요하다면, 위 사항을 편집한다.
아래의 명령으로 Local Storm cluster에서 테스트할 수 있다.

> gradle test --tests *.SimpleMeetUpParseTopologyTest

### Run in a remote Storm
원격(172.17.8.101)에서 실행중인 Storm에서 위 topology를 실행하기 위해서는 **strom** 커맨드를 이용하여야 한다. 즉, storm이 로컬 머신에 설치되어 있어야 하고, [storm_home_directory]/conf/storm.yaml의 내용이 아래와 같으면 된다.

    storm.zookeeper.servers:
        - "172.17.8.101"
    nimbus.host: "172.17.8.101"
    drpc.servers:
        - "172.17.8.101"

위 사항을 확인하였으면 아래의 명령을 차례로 수행한다.
storm-topology 디렉토리 안에서;
> gradle shadowJar

> storm jar ./build/libs/storm-topology-0.8-SNAPSHOT-all.jar com.obzen.stream.storm.SimpleMeetUpParseTopology -z 172.17.8.101:2181 -k 172.17.8.101:9092 -s meetup_venue_lines -t meetup_venue_events

위에서 두번째 커맨드는 원격 Storm (conf/storm.yaml 에 셋팅된)으로 topology(Jar 파일)을 전송하되, 실행될 토폴로지에 필요한 파라미터들을 함께 전달하기 위한 것이다. 
파라미터들에 대해서는 **SimpleMeetUpParseTopology.java**의 소스를 확인해보면 알 수 있다.

Storm클러스터에 deploy된 토폴로지들의 확인;

> storm list

토폴로지를 중단하고 싶으면, 실행 중인 토폴로지의 이름을 이용한다;

> storm kill meetup-venue-topology

## cep-query
-----------------------------------
### Sample Continuous Query
**cep-query/src/main/resources/meetup_event_cq.json** 파일을 열어 obzenCEP에서 실행될 continuous query를 살펴보도록 하자.

    {
        "definitionMeta" : {
            "definitionId" : {
                "name" : "meetup-event-cq",
                "version" : "0.1"
            },
            "description" : "For fun"
        },
        "service" : "ocep.EventProcessor",
        "startupOptions" : {
            "config" : {
                "inputPorts" : [ {
                    "adapterProviderName" : "KafkaAdapterProvider",
                    "portName" : "meetup_venue_events",
                    "streamId" : "venueStream"
                } ],
                "outputPorts" : [ {
                    "adapterProviderName" : "KafkaAdapterProvider",
                    "portName" : "meetup_venue_out",
                    "streamId" : "countryAndCityStream"
                } ],
                "executionPlan" : [
                                    "@Plan:name('SimpleVenueCQ01')",
                                    "define stream venueStream (zip string, country string, city string, address_1 string, name string, id long, mtime long);",
                                    "   @info(name = 'query') ",
                                    "   from venueStream[not(zip is null)]#window.time(1 min)",
                                    "   select country, city, count(city) as cityCount",
                                    "   group by country, city",
                                    "   having cityCount > 10",
                                    "   insert current events into countryAndCityStream ;"
                ],
                "useJunction" : false,
                "snapshotIntervalMinutes" : 5,
                "snapshotExpireMinutes" : 1440,
                "distributedExecution" : false,
                "enableStatistics" : false
            }
        }
    }

위 내용을 간략히 살펴보면,

* **definitionMeta** : 서버에 탑재되었을 때에 식별가능한 ID를 정의하고 있다. ID = name + version
* **service** : Event Processor의 서비스 이름은 언제나 **ocep.EventProcessor**
* **startupOptions/config/executionPlan** : 가장 중요한 부분으로 Siddhi Library를 이용하여 수행될 Continuous Query를 정의한다.<br> 이 Query의 ```define stream ... ```절로부터, input stream으로 ```venueStream```이 데이터 구조와 함께 정의되어 있는 것을 확인할 수 있다.<br> 또한  ```insert into countryAndCityStream```절로부터 output stream인 ```countryAndCityStream```이 (암묵적으로)정의되고 있으며,<br> ```select country, city, count(city) as cityCount```로부터 output stream의 데이터 구조가 [String, String, Long]인 것 또한 (SiddhiQL의 데이터 타입 컨버전 규칙에 따라) 유추할 수 있다.
* **startupOptions/config/inputPorts** : ```KafkaAdatperProvider```를 이용하여 ```meetup_venue_events``` (portName == topic name)를 읽어들여, executionPlan의 input stream인 ```venueStream```으로 전달한다.
* **startupOptions/config/outputPorts** : ```KafkaAdapterProvider```를 이용하여 executionPlan의 output stream인 ```countryAndCityStream```의 데이터를 ```meetup_venue_out```(portName == topic name)으로 전달한다.

### Run in a remote obzenCEP
위 Json 파일의 내용은 obzenCEP에서 제공하는 RESTful API를 이용하여 obzenCEP로 전송되어야 한다.<br> 
이 때 URL end point는 **http://172.17.8.101:8082/rest/nodes/node.172.17.8.101.5800/services** 이고, HTTTP method는 PUT을 이용한다.<br>
이를 위한 간단한 프로그램이 **cep-query/src/main/java/com/obzen/cep/AdminRestAPITester.java** 이다.

이 프로그램을 Gradle에서 수행하기 위해서는 다음의 명령을 사용한다.
cep-query 디렉토리 안에서;

> gradle run -Pargs="-c **start** -e 172.17.8.101:8082 -n node.172.17.8.101.5800 -j ./src/main/resources/meetup_event_cq.json"

위의 명령으로 실행시킨 쿼리를 중단하기 위해서는 위 구문의 start를 stop으로 바꾸어 실행하면 된다.

> gradle run -Pargs="-c **stop** -e 172.17.8.101:8082 -n node.172.17.8.101.5800 -j ./src/main/resources/meetup_event_cq.json"

참고로 AdminRestAPITester는 obzenCEP의 RESTful API들 중 위의 두 가지 경우만을 테스트할 수 있다. 나머지 API들에 대해서는 obzenCEP 프로젝트를 참조한다.

## console-reader
------------------------------------
CEP의 처리결과를 조회하기 위한 간단한 Kafka Reader이다.<br>
위에서와 마찬가지로 build.gradle 의 mainClassName에 해당하는 프로그램의 소스를 살펴보면, 어떻게 Kafka Topic으로부터, CEP Query의 output stream을 읽어들일 수 있는지 알 수 있다.

### Run in Gradle

> gradle run -Pargs="-z 172.17.8.101:2181 -k 172.17.8.101:9092 -t meetup_venue_out -g test-group"
