# DOCKER-VERSION 1.9.1
# Event Feed from MeetUp
# tag: obzen-reg:5000/obzen/meetup-feed:0.8

FROM obzen-reg:5000/obzen/alpine-oracle-jdk8-stripped:3.3

# bash needed
RUN apk --update add bash && rm -rf /var/cache/apk/*

# from the tar archive
COPY  ./event-feeder-0.8-SNAPSHOT.tar /tmp/event-feeder-0.8-SNAPSHOT.tar

RUN tar xvf /tmp/event-feeder-0.8-SNAPSHOT.tar -C /opt \
    && ln -s /opt/event-feeder-0.8-SNAPSHOT /opt/event-feeder \
    && rm /tmp/event-feeder-0.8-SNAPSHOT.tar

WORKDIR /opt/event-feeder

CMD ["/opt/event-feeder/bin/event-feeder"]
