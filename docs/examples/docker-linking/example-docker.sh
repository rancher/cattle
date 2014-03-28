#!/bin/bash

echo Start Logstash
docker run -d --name logstash cattle/logstash

echo Start Kibana
docker run -d -p 80 --name kibana --link logstash:es cattle/kibana

echo Start MySQL
docker run -d -p 80 --name mysql cattle/mysql

echo Start Cattle
docker run -d -p 8080 --name cattle --link mysql:mysql --link logstash:gelf cattle/cattle 
