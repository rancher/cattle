#!/usr/bin/env python

import os
import etcd
import time
import json

template = """
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/lib/zookeeper
clientPort=2181
autopurge.snapRetainCount=3
autopurge.purgeInterval=1

"""

e = etcd.Etcd(host='172.17.42.1')

KEY = 'zoo-' + os.environ["ETCD_KEY"]

def registry():
    print 'Registering Config'

    servers = []
    i = 1
    while True:
        try:
            servers.append(os.environ['ZOOKEEPER_' + str(i)])
        except:
            break
        i = i + 1
    
    e.set(KEY, json.dumps(servers))


def server():
    print 'Looking for Config'

    server_id = os.environ['ZOOKEEPER_SERVER_ID']

    if not os.path.exists('/var/lib/zookeeper'):
        os.mkdir('/var/lib/zookeeper')

    with open('/var/lib/zookeeper/myid', 'w') as f:
        f.write(server_id)

    while True:
        try:
            servers = json.loads(e.get(KEY).value)
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

    os.execl('/zookeeper/bin/zkServer.sh', 'zkServer.sh', 'start-foreground')


if 'ZOOKEEPER_1' in os.environ:
    registry()
else:
    server()
