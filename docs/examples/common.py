from dstack import from_env
from time import sleep

client = None


def create_client(url):
    global client
    client = from_env(url=url)
    return client


def get_url(obj, port):
    obj = wait_done(obj)
    return 'http://{}:{}'.format(obj.dockerHostIp,
                                 obj.dockerPorts[port])


def wait_done(obj):
    i = 0
    obj = client.reload(obj)
    while obj.transitioning == 'yes':
        i += 1
        if i % 10 == 0:
            print 'Waiting on {0}'.format(obj.name)
        sleep(0.5)
        obj = client.reload(obj)

    if obj.transitioning == 'error':
        raise Exception(obj.transitioningMessage)

    return obj


def link(env={}, **kw):
    result = dict(env)

    for name, obj in kw.items():
        obj = wait_done(obj)

        if 'dockerPorts' not in obj:
            continue

        ip = obj.dockerHostIp
        for src, dst in obj.dockerPorts.items():
            port, proto = src.split('/', 1)

            result['{}_NAME'.format(name).upper()] = '/self/{}'.format(name)
            result['{}_PORT'.format(name).upper()] = \
                '{}://{}:{}'.format(proto, ip, dst)
            result['{}_PORT_{}_{}'.format(name, port, proto).upper()] = \
                '{}://{}:{}'.format(proto, ip, dst)
            result['{}_PORT_{}_{}_ADDR'.format(name, port, proto).upper()] = ip
            result['{}_PORT_{}_{}_PORT'.format(name, port, proto).upper()] = \
                dst
            result['{}_PORT_{}_{}_PROTO'.format(name, port, proto).upper()] = \
                proto

    return result
