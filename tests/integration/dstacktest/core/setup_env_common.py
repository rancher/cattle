import uuid
import sys
from time import sleep
from dstack import from_env


hosts = from_env('DSTACK').list_host(state='active', kind='docker')
if len(hosts) == 0:
    print "You have no docker hosts registered"
    sys.exit(1)

host_index = 0
created = []


def zookeeper_env(client, zookeeper_servers):
    ret = {}

    for i, z in enumerate(zookeeper_servers):
        z = wait_done(client, z)
        index = str(i+1)

        ret['ZOOKEEPER_' + index] = \
            '{}:{}:{}'.format(z.dockerHostIp,
                              z.dockerPorts['2888/tcp'],
                              z.dockerPorts['3888/tcp'])

        ret['ZOOKEEPER_CONN_STRING_' + index] = \
            '{}:{}'.format(z.dockerHostIp,
                           z.dockerPorts['2181/tcp'])

    return ret


def create_servers(client, count, name=None, common=None, env=None,
                   image_uuid='docker:ibuildthecloud/dstack', wait_first=False,
                   ports=None, profile=None):
    if env is None:
        env = {}

    result = []
    for i in range(count):
        server_env = env
        server_name = '{} {}'.format(name, i + 1)

        if profile is not None:
            server_name = '{}-{}'.format(profile, i + 1)
            server_env = merge(server_env, {
                'DSTACK_SERVER_PROFILE': profile,
                'DSTACK_DSTACK_SERVER_ID': server_name.replace('-', '')
            }, 0)

        kw = {
            'requestedHostId': next_host(),
            'name': server_name,
            'imageUuid': image_uuid,
            'environment': merge(common, server_env, i + 1)
        }

        if ports is not None:
            kw['ports'] = ports

        container = client.create_container(**kw)

        if i == 0 and wait_first:
            container = wait_done(client, container)

        result.append(container)

    created.extend(result)
    return result


def merge(one, two, index):
    if one is None:
        one = {}
    if two is None:
        two = {}

    ret = {}
    if two is None:
        return dict(one)

    for k, v in dict(one.items() + two.items()).items():
        if v == '${INDEX}':
            v = index
        ret[k] = v

    return ret


def wait_done(client, obj):
    i = 0
    obj = client.reload(obj)
    while obj.transitioning == 'yes':
        i += 1
        if i % 10 == 0:
            print 'Waiting on {0}'.format(obj.name)
        sleep(0.5)
        obj = client.reload(obj)

    return obj


def purge_old_containers(client):
    did_something = False
    for c in client.list_container(removed_null=True):
        if c.state == 'running':
            print 'Stopping container', c.id
            did_something = True
            c.stop(remove=True)

    if did_something:
        sys.exit(0)


def next_host():
    global host_index
    ret = hosts[host_index % len(hosts)].id
    host_index += 1
    return ret


def print_servers(client):
    for s in created:
        wait_done(client, s)

    for s in created:
        s = wait_done(client, s)
        uri = s.instanceHostMaps()[0].host().agent().uri
        print 'Created {} on {}'.format(s.name, uri)


def get_host_port(container, port):
    return '{}:{}'.format(container.dockerHostIp,
                          container.dockerPorts[port])


def get_http_url(container, port):
    return 'http://{}:{}'.format(container.dockerHostIp,
                                 container.dockerPorts[port])


def setup_zookeeper(client, env):
    image_uuid = 'docker:ibuildthecloud/zookeeper'
    zookeeper_bootstrap = create_servers(client, 1, 'ZooKeeper BootStrap',
                                         image_uuid=image_uuid,
                                         ports=[2181])[0]

    zookeeper_bootstrap = wait_done(client, zookeeper_bootstrap)

    common = {
        'ZOO_KEY': str(uuid.uuid4()),
        'ZOOKEEPER_BOOTSTRAP': get_host_port(zookeeper_bootstrap, '2181/tcp')
    }

    zookeeper_servers = create_servers(client, 3, 'ZooKeeper', common, env={
        'ZOOKEEPER_SERVER_ID': '${INDEX}'
    }, image_uuid=image_uuid,
        ports=[2181, 2888, 3888])

    zk_env = zookeeper_env(client, zookeeper_servers)

    create_servers(client, 1, 'ZooKeeper Registry', common, env=zk_env,
                   image_uuid=image_uuid)

    zk_conn_string = ','.join(
        [v for k, v in zk_env.items() if 'ZOOKEEPER_CONN' in k])
    print 'ZooKeeper Connection String:', zk_conn_string

    env['DSTACK_ZOOKEEPER_CONNECTION_STRING'] = zk_conn_string
    env['DSTACK_MODULE_PROFILE_ZOOKEEPER'] = 'true'

    return zookeeper_servers


def setup_redis(client, env):
    redis_servers = create_servers(client, 3, 'Redis', {}, env={},
                                   image_uuid='docker:ibuildthecloud/redis',
                                   ports=[6379])
    hosts = []
    for r in redis_servers:
        r = wait_done(client, r)
        hosts.append('{}:{}'.format(r.dockerHostIp, r.dockerPorts['6379/tcp']))

    redis_hosts = ','.join(hosts)
    print 'Redis Connection String:', redis_hosts

    env['DSTACK_REDIS_HOSTS'] = redis_hosts
    env['DSTACK_MODULE_PROFILE_REDIS'] = 'true'

    return redis_servers


def setup_haproxy(client, api_servers):
    env = {}

    for i, s in enumerate(api_servers):
        s = wait_done(client, s)
        index = str(i+1)
        target = '{}:{}'.format(s.dockerHostIp,
                                s.dockerPorts['8080/tcp'])
        env['TARGET' + index + '_PORT'] = target

    haproxy = create_servers(client, 1, 'HAProxy', env, env={},
                             image_uuid='docker:ibuildthecloud/haproxy:1',
                             ports=[80])[0]

    haproxy = wait_done(client, haproxy)

    print 'Created HA Proxy', get_http_url(haproxy, '80/tcp')

    return haproxy


def setup_logstash(client, env):
    imageUuid = 'docker:ibuildthecloud/logstash'
    logstash = client.create_container(name='Logstash',
                                       requestedHostId=next_host(),
                                       imageUuid=imageUuid)
    logstash = wait_done(client, logstash)

    print 'Created Logstash', get_host_port(logstash, '12201/udp')

    env['DSTACK_LOGBACK_OUTPUT_GELF'] = 'true'
    env['DSTACK_LOGBACK_OUTPUT_GELF_HOST'] = logstash.dockerHostIp
    env['DSTACK_LOGBACK_OUTPUT_GELF_PORT'] = logstash.dockerPorts['12201/udp']

    created.append(logstash)
    return logstash


def setup_h2(client, env):
    if 'DSTACK_DB_DSTACK_DATABASE' in env:
        return

    db = client.create_container(
        requestedHostId=next_host(),
        name='H2 Database Server',
        imageUuid='docker:ibuildthecloud/dstack',
        environment={
            'JAVA_OPTS': '-Xmx1024m -Dmain=org.h2.tools.Server',
            'ARGS': '-webAllowOthers -tcpAllowOthers'
        },
        tcpPorts=[9092, 8082])

    db = wait_done(client, db)
    db_url = 'tcp://{0}:{1}/'.format(db.dockerHostIp,
                                     db.dockerPorts['9092/tcp'])
    web_url = 'http://{0}:{1}'.format(db.dockerHostIp,
                                      db.dockerPorts['8082/tcp'])

    print 'Created DB', web_url, db_url, db.links.self

    env['DSTACK_DB_DSTACK_DATABASE'] = 'h2'
    env['DSTACK_DB_DSTACK_H2_REMOTE_URL'] = db_url
    env['DSTACK_DB_DSTACK_USER'] = 'dstack'
    env['DSTACK_DB_DSTACK_PASSWORD'] = 'dstack'
    env['DSTACK_DB_RELEASE_CHANGE_LOCK'] = 'false'

    created.append(db)
    return db


def setup_graphite(client, env):
    image_uuid = 'docker:ibuildthecloud/graphite'
    graphite = client.create_container(name='Graphite',
                                       requestedHostId=next_host(),
                                       imageUuid=image_uuid)
    graphite = wait_done(client, graphite)

    print 'Created Graphite', get_http_url(graphite, '80/tcp')

    env['DSTACK_GRAPHITE_HOST'] = graphite.dockerHostIp
    env['DSTACK_GRAPHITE_PORT'] = graphite.dockerPorts['2003/tcp']

    created.append(graphite)
    return graphite


def setup_kibana(logstash, client, env):
    image_uuid = 'docker:ibuildthecloud/kibana'
    kibana_env = {
        'ES_PORT_9200_TCP_ADDR': logstash.dockerHostIp,
        'ES_PORT_9200_TCP_PORT': logstash.dockerPorts['9200/tcp']
    }

    kibana = client.create_container(name='Kibana',
                                     requestedHostId=next_host(),
                                     imageUuid=image_uuid,
                                     environment=kibana_env)
    kibana = wait_done(client, kibana)

    print 'Created Kibana', get_http_url(kibana, '80/tcp')

    created.append(kibana)
    return kibana
