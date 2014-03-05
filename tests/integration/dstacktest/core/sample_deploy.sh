#!/bin/bash

echo Start Logstash
docker run -d --name logstash ibuildthecloud/logstash

echo Start Kibana
docker run -d -P --name kibana --link logstash:es ibuildthecloud/kibana

echo Start MySQL
docker run -d -P --name mysql ibuildthecloud/mysql

echo Start dStack
docker run -d -P --name dstack --link mysql:mysql --link logstash:gelf ibuildthecloud/dstack 
