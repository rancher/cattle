#!/usr/bin/env python

import os
import time
import json
from kazoo.client import KazooClient

template = '''
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/lib/zookeeper
clientPort=2181
autopurge.snapRetainCount=3
autopurge.purgeInterval=1

'''

KEY = '/zoo-' + str(os.environ.get('KEY'))
zk = None


def from_env(name, server=False):
    if server:
        return '{}:{}:{}'.format(os.environ[name + '_PORT_2888_TCP_ADDR'],
                                 os.environ[name + '_PORT_2888_TCP_PORT'],
                                 os.environ[name + '_PORT_3888_TCP_PORT'])
    else:
        return '{}:{}'.format(os.environ[name + '_PORT_2181_TCP_ADDR'],
                              os.environ[name + '_PORT_2181_TCP_PORT'])


def connect():
    global zk

    if zk is None:
        try:
            host = from_env('BOOTSTRAP')
            print 'ZooKeeper Bootstrap', host
            zk = KazooClient(hosts=host)
            zk.start()
        except:
            zk = None


def put(data):
    connect()
    print 'Writing', data, 'to', KEY
    zk.create(KEY, data)


def get():
    connect()
    data, stat = zk.get(KEY)
    val = data.decode('utf-8')
    print 'Got', val, 'from', KEY
    return val


def registry():
    print 'Registering Config'

    servers = []
    i = 1
    while True:
        try:
            servers.append(from_env('ZK{}'.format(i), server=True))
        except:
            break
        i = i + 1
    
    put(json.dumps(servers))


def server():
    server_id = os.environ.get('ID', '1')

    print 'Server ID', server_id

    if not os.path.exists('/var/lib/zookeeper'):
        os.mkdir('/var/lib/zookeeper')

    with open('/var/lib/zookeeper/myid', 'w') as f:
        f.write(server_id)

    servers = []

    if 'ID' in os.environ:
        print 'Looking for Config'

        while True:
            try:
                servers = json.loads(get())
                break
            except:
                print 'Waiting for config'
                time.sleep(1)

    with open('/zookeeper/conf/zoo.cfg','w') as f:
        f.write(template)

        for i, server in enumerate(servers):
            id = str(i+1)
            if id == server_id:
                f.write('server.{0}={1}:2888:3888\n'.format(id, server.split(':', 1)[0]))
            else:
                f.write('server.{0}={1}\n'.format(id, server))

    with open('/zookeeper/conf/zoo.cfg') as f:
        print 'Config'
        print f.read()

    os.execl('/zookeeper/bin/zkServer.sh', 'zkServer.sh', 'start-foreground')


if 'ZK1_PORT_2888_TCP_ADDR' in os.environ:
    registry()
else:
    server()
