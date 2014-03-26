#!/bin/bash

echo Start Logstash
docker run -d --name logstash ibuildthecloud/logstash

echo Start Kibana
docker run -d -p 80 --name kibana --link logstash:es ibuildthecloud/kibana

echo Start MySQL
docker run -d -p 80 --name mysql ibuildthecloud/mysql

echo Start dStack
docker run -d -p 8080 --name dstack --link mysql:mysql --link logstash:gelf ibuildthecloud/dstack 
