#!/bin/bash
set -e

sed -i -e 's/window.location.hostname/"'${ES_PORT_9200_TCP_ADDR}'"/g' \
       -e 's/9200/'${ES_PORT_9200_TCP_PORT}'/g' /var/www/html/config.js

/etc/init.d/apache2 start

while [ -e /proc/$(</run/apache2/apache2.pid) ]; do
    sleep 15
done
