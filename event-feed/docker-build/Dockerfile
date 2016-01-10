# DOCKER-VERSION 1.9.1
# Event Feed from MeetUp
# tag: obzen-reg:5000/obzen/meetup-feed:0.8

FROM obzen-reg:5000/obzen/alpine-oracle-jdk8-stripped:3.3

# bash needed
RUN apk --update add bash && rm -rf /var/cache/apk/*

# from the tar archive
COPY  ./event-feed-0.8-SNAPSHOT.tar /tmp/event-feed-0.8-SNAPSHOT.tar

RUN tar xvf /tmp/event-feed-0.8-SNAPSHOT.tar -C /opt \
    && ln -s /opt/event-feed-0.8-SNAPSHOT /opt/event-feed \
    && rm /tmp/event-feed-0.8-SNAPSHOT.tar

WORKDIR /opt/event-feed

CMD ["/opt/event-feed/bin/event-feed"]
