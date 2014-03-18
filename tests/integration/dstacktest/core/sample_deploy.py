#!/usr/bin/env python
import os
from uuid import uuid4
from common import create_client, link, wait_done, get_url

OTHER_SERVERS = {
    'api1': 'api-server',
    'api2': 'api-server',
    'api3': 'api-server',
    'process1': 'process-server',
    'process2': 'process-server',
    'process3': 'process-server',
    'agent1': 'agent-server',
    'agent2': 'agent-server',
}

client = create_client(os.environ.get('DSTACK_URL', 'http://localhost:8080'))

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

print 'Creating Redis 1'
redis1 = client.create_container(name='redis1',
                                 imageUuid='docker:ibuildthecloud/redis')

print 'Creating Redis 2'
redis2 = client.create_container(name='redis2',
                                 imageUuid='docker:ibuildthecloud/redis')

print 'Creating Redis 3'
redis3 = client.create_container(name='redis3',
                                 imageUuid='docker:ibuildthecloud/redis')

print 'Creating ZooKeeper Bootstrap'
key = str(uuid4())
zk_boot = client.create_container(name='zk_boot',
                                  imageUuid='docker:ibuildthecloud/zookeeper')

print 'Creating ZooKeeper 1'
zk1 = client.create_container(name='zk1',
                              imageUuid='docker:ibuildthecloud/zookeeper',
                              environment=link(bootstrap=zk_boot))

print 'Creating ZooKeeper 2'
zk2 = client.create_container(name='zk2',
                              imageUuid='docker:ibuildthecloud/zookeeper',
                              environment=link(bootstrap=zk_boot))

print 'Creating ZooKeeper 3'
zk3 = client.create_container(name='zk3',
                              imageUuid='docker:ibuildthecloud/zookeeper',
                              environment=link(bootstrap=zk_boot))

print 'Creating ZooKeeper Registry'
zk3 = client.create_container(name='zk_reg',
                              imageUuid='docker:ibuildthecloud/zookeeper',
                              environment=link(zk1=zk1,
                                               zk2=zk2,
                                               zk3=zk3))

servers = {}

for name, profile in OTHER_SERVERS.items():
    print 'Creating dStack', name
    env = {
        'DSTACK_SERVER_PROFILE': profile,
        'DSTACK_SERVER_ID': name
    }
    server = client.create_container(name=name,
                                     imageUuid='docker:ibuildthecloud/dstack',
                                     environment=link(env=env,
                                                      zk1=zk1,
                                                      zk2=zk2,
                                                      zk3=zk3,
                                                      redis1=redis1,
                                                      redis2=redis2,
                                                      redis3=redis3,
                                                      mysql=db,
                                                      gelf=logstash))
    servers[name] = server

print 'Creating HA Proxy'
haproxy = client.create_container(name='haproxy',
                                  imageUuid='docker:ibuildthecloud/haproxy',
                                  environment=link(target1=servers['api1'],
                                                   target2=servers['api2'],
                                                   target3=servers['api3']))

wait_done(haproxy)

print 'phpMyAdmin running at {}/phpmyadmin'.format(get_url(db, '80/tcp'))
print 'Kibana running at', get_url(kibana, '80/tcp')
print 'dStack running at', get_url(haproxy, '80/tcp')
