from common import client, link, wait_done, get_url

print 'Creating logstash'
logstash = client.create_container(name='logstash',
                                   imageUuid='docker:ibuildthecloud/logstash')

print 'Creating Kibana'
kibana = client.create_container(name='kibana',
                                 imageUuid='docker:ibuildthecloud/kibana',
                                 environment=link(es=logstash))

print 'Creating MySQL'
db = client.create_container(name='mysql',
                             imageUuid='docker:ibuildthecloud/mysql')

print 'Creating dStack'
dstack = client.create_container(name='dstack',
                                 imageUuid='docker:ibuildthecloud/dstack',
                                 environment=link(mysql=db,
                                                  gelf=logstash))
dstack = wait_done(dstack)

print 'phpMyAdmin running at {}/phpmyadmin'.format(get_url(db, '80/tcp'))
print 'Kibana running at', get_url(kibana, '80/tcp')
print 'dStack running at', get_url(dstack, '8080/tcp')
