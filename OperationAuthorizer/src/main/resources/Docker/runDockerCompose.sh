#!/usr/bin/env bash

docker-compose -f hazelcast.yml up -d && docker-compose -f hazelcast.yml exec -T authorizer java -jar /usr/local/lib/OperationAuthorizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
#&& docker exec authorizer java -jar /usr/local/lib/OperationAuthorizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar

