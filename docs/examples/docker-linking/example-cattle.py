#!/usr/bin/env python
from common import create_client, link, wait_done, get_url

client = create_client('http://localhost:8080')

print 'Creating logstash'
logstash = client.create_container(name='logstash',
                                   imageUuid='docker:cattle/logstash')

print 'Creating Kibana'
kibana = client.create_container(name='kibana',
                                 imageUuid='docker:cattle/kibana',
                                 environment=link(es=logstash))

print 'Creating MySQL'
db = client.create_container(name='mysql',
                             imageUuid='docker:cattle/mysql')

print 'Creating Cattle'
cattle = client.create_container(name='cattle',
                                 imageUuid='docker:cattle/cattle',
                                 environment=link(mysql=db,
                                                  gelf=logstash))
cattle = wait_done(cattle)

print 'phpMyAdmin running at {}/phpmyadmin'.format(get_url(db, '80/tcp'))
print 'Kibana running at', get_url(kibana, '80/tcp')
print 'Cattle running at', get_url(cattle, '8080/tcp')
