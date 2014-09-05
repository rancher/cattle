#!/usr/bin/env python

import os
import sys
import socket
import requests

LOCAL_IP = 'http://169.254.169.254/latest/meta-data/local-ipv4'

template = '''
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/dev/zk
clientPort=2181
autopurge.snapRetainCount=1
autopurge.purgeInterval=1
forceSync=no

'''


def from_env(name):
    return '{}:{}:{}'.format(os.environ[name + '_PORT_2888_TCP_ADDR'],
                                os.environ[name + '_PORT_2888_TCP_PORT'],
                                os.environ[name + '_PORT_3888_TCP_PORT'])


def get_servers(id):
    print 'Looking up server'

    servers = []
    i = 1
    while True:
        try:
            if i == id:
                ip = requests.get(LOCAL_IP).text
                servers.append('{}:2888:3888'.format(ip))
            else:
                servers.append(from_env('ZK{}'.format(i)))
        except:
            break
        i = i + 1
    
    print 'Servers', servers
    return servers


def server():
    server_id = int(os.environ.get('ID', '1'))

    print 'Server ID', server_id

    # /dev is an unconstrained tmpfs in Docker, /dev/shm is 64mb
    if not os.path.exists('/dev/zk'):
        os.mkdir('/dev/zk')

    with open('/dev/zk/myid', 'w') as f:
        f.write(str(server_id))

    servers = get_servers(server_id)

    with open('/zookeeper/conf/zoo.cfg','w') as f:
        f.write(template)

        for i, server in enumerate(servers):
            id = str(i+1)
            f.write('server.{0}={1}\n'.format(id, server))

    with open('/zookeeper/conf/zoo.cfg') as f:
        print 'Config'
        print f.read()

    sys.stdout.flush()
    env = dict(os.environ)
    env['JVMFLAGS'] = '-Djava.net.preferIPv4Stack=true'

    os.execle('/zookeeper/bin/zkServer.sh', 'zkServer.sh', 'start-foreground', env)

server()
