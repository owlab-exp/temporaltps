#!/bin/sh
TAG=obzen-reg:5000/obzen/event-feed:0.8
docker build -t ${TAG} . &&  docker push ${TAG}
