from docker import Client, APIError

# TODO cattle.plugins.load_plugins() somehow make cattle.plugin.* modules
# unavailable, importing it first
import cattle.plugins.docker  # NOQA

from .common_fixtures import *  # NOQA
import pytest
from cattle import CONFIG_OVERRIDE, Config


if_docker = pytest.mark.skipif('os.environ.get("DOCKER_TEST") != "true"',
                               reason='DOCKER_TEST is not set')


CONFIG_OVERRIDE['DOCKER_HOST_IP'] = '1.2.3.4'


def _delete_container(name):
    client = Client()
    for c in client.containers(all=True):
        for container_name in c['Names']:
            if name == container_name:
                try:
                    client.kill(c)
                except:
                    pass
                client.remove_container(c)


def _get_container(name):
    client = Client()
    for c in client.containers(all=True):
        for container_name in c['Names']:
            if name == container_name:
                return c
    return None


@if_docker
def test_image_list():
    c = Client()
    images = c.images(all=True)
    if len(images) == 0:
        c.pull('busybox')

    images = c.images(all=True)

    assert 'Id' in images[0]
    assert 'ID' not in images[0]


@if_docker
def test_image_activate(agent, responses):
    try:
        Client().remove_image('ibuildthecloud/helloworld:latest')
    except APIError:
        pass

    def post(req, resp):
        del resp['data']['+data']['dockerImage']['VirtualSize']

    event_test(agent, 'docker/image_activate', post_func=post)


@if_docker
def test_volume_activate(agent, responses):
    event_test(agent, 'docker/volume_activate')


@if_docker
def test_volume_deactivate(agent, responses):
    event_test(agent, 'docker/volume_deactivate')


@if_docker
def test_instance_activate_need_pull_image(agent, responses):
    try:
        Client().remove_image('ibuildthecloud/helloworld:latest')
    except APIError:
        pass

    test_instance_only_activate(agent, responses)


@if_docker
def test_instance_only_activate(agent, responses):
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

    event_test(agent, 'docker/instance_activate', post_func=post)


@if_docker
def test_instance_activate_ports(agent, responses):
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del fields['dockerIp']

        assert len(docker_container['Ports']) == 1
        assert docker_container['Ports'][0]['PrivatePort'] == 8080
        assert docker_container['Ports'][0]['Type'] == 'tcp'

    event_test(agent, 'docker/instance_activate_ports', post_func=post)


@if_docker
def test_instance_activate_links(agent, responses):
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        id = docker_container['Id']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

        inspect = Client().inspect_container(id)

        env = inspect['Config']['Env']

        assert 'MYSQL_NAME=/cattle/mysql' in env
        assert 'MYSQL_PORT=udp://127.0.0.2:12346' in env
        assert 'MYSQL_PORT_3307_UDP=udp://127.0.0.2:12346' in env
        assert 'MYSQL_PORT_3307_UDP_ADDR=127.0.0.2' in env
        assert 'MYSQL_PORT_3307_UDP_PORT=12346' in env
        assert 'MYSQL_PORT_3307_UDP_PROTO=udp' in env

        assert 'MYSQL_PORT_3306_TCP=tcp://127.0.0.1:12345' in env
        assert 'MYSQL_PORT_3306_TCP_ADDR=127.0.0.1' in env
        assert 'MYSQL_PORT_3306_TCP_PORT=12345' in env
        assert 'MYSQL_PORT_3306_TCP_PROTO=tcp' in env

        assert 'REDIS_NAME=/cattle/redis' in env
        assert 'REDIS_PORT=udp://127.0.0.1:23456' in env
        assert 'REDIS_PORT_26_UDP=udp://127.0.0.1:23456' in env
        assert 'REDIS_PORT_26_UDP_ADDR=127.0.0.1' in env
        assert 'REDIS_PORT_26_UDP_PORT=23456' in env
        assert 'REDIS_PORT_26_UDP_PROTO=udp' in env

    event_test(agent, 'docker/instance_activate_links', post_func=post)


@if_docker
def test_instance_activate_links_no_service(agent, responses):
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')
    _delete_container('/target_redis')
    _delete_container('/target_mysql')

    client = Client()
    c = client.create_container('ibuildthecloud/helloworld',
                                ports=['3307/udp', '3306/tcp'],
                                name='target_mysql')
    client.start(c, port_bindings={
        '3307/udp': ('127.0.0.2', 12346),
        '3306/tcp': ('127.0.0.2', 12345)
    })

    c = client.create_container('ibuildthecloud/helloworld',
                                name='target_redis')
    client.start(c)

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        id = docker_container['Id']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

        inspect = Client().inspect_container(id)

        # TODO: This seems like a bug in docker, but 'HostConfig.Links' is
        # never set
        assert inspect['HostConfig']['Links'] is None

    event_test(agent, 'docker/instance_activate_links_no_service',
               post_func=post)


@if_docker
def test_instance_activate_ipsec(agent, responses):
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][2]['PublicPort']
        del docker_container['Ports'][3]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

    event_test(agent, 'docker/instance_activate_ipsec', post_func=post)


@if_docker
def test_instance_activate_agent_instance_localhost(agent, responses):
    CONFIG_OVERRIDE['CONFIG_URL'] = 'https://localhost:1234/a/path'
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):

        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        id = docker_container['Id']

        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

        inspect = Client().inspect_container(id)

        port = Config.api_proxy_listen_port()
        assert 'CATTLE_CONFIG_URL_SCHEME=https' in inspect['Config']['Env']
        assert 'CATTLE_CONFIG_URL_PATH=/a/path' in inspect['Config']['Env']
        assert 'CATTLE_CONFIG_URL_PORT={0}'.format(port) in \
            inspect['Config']['Env']

    event_test(agent, 'docker/instance_activate_agent_instance',
               post_func=post)


@if_docker
def test_instance_activate_agent_instance(agent, responses):
    CONFIG_OVERRIDE['CONFIG_URL'] = 'https://something.fake:1234/a/path'
    _delete_container('/c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):

        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        id = docker_container['Id']

        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

        inspect = Client().inspect_container(id)

        port = Config.api_proxy_listen_port()
        assert 'CATTLE_CONFIG_URL={0}'.format(Config.config_url()) in \
               inspect['Config']['Env']
        assert 'CATTLE_CONFIG_URL_SCHEME=https' not in inspect['Config']['Env']
        assert 'CATTLE_CONFIG_URL_PATH=/a/path' not in inspect['Config']['Env']
        assert 'CATTLE_CONFIG_URL_PORT={0}'.format(port) not in \
               inspect['Config']['Env']
        assert 'ENV1=value1' in inspect['Config']['Env']

    event_test(agent, 'docker/instance_activate_agent_instance',
               post_func=post)


@if_docker
def test_instance_activate_command(agent, responses):
    _delete_container('/c-c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

    event_test(agent, 'docker/instance_activate_command', post_func=post)


@if_docker
def test_instance_activate_command_args(agent, responses):
    _delete_container('/ca-c861f990-4472-4fa1-960f-65171b544c28')

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

    event_test(agent, 'docker/instance_activate_command_args', post_func=post)


@if_docker
def test_instance_deactivate(agent, responses):
    CONFIG_OVERRIDE['STOP_TIMEOUT'] = 1

    test_instance_only_activate(agent, responses)

    def post(req, resp):
        del resp['data']['instance']['+data']['dockerContainer']['Created']
        del resp['data']['instance']['+data']['dockerContainer']['Id']
        del resp['data']['instance']['+data']['dockerContainer']['Status']
        del resp['data']['instance']['+data']['+fields']['dockerIp']

    event_test(agent, 'docker/instance_deactivate', post_func=post)


@if_docker
def test_ping(agent, responses):
    test_instance_only_activate(agent, responses)

    CONFIG_OVERRIDE['DOCKER_UUID'] = 'testuuid'
    CONFIG_OVERRIDE['PHYSICAL_HOST_UUID'] = 'hostuuid'

    def post(req, resp):
        hostname = Config.hostname() + '/docker'
        pool_name = hostname + ' Storage Pool'
        resources = resp['data']['resources']

        uuid = 'c861f990-4472-4fa1-960f-65171b544c28'
        instances = filter(lambda x: x['type'] == 'instance' and
                           x['uuid'] == uuid, resources)
        assert len(instances) == 1

        resources = filter(lambda x: x.get('kind') == 'docker', resources)
        resources.append(instances[0])

        resp['data']['resources'] = resources

        assert resp['data']['resources'][0]['name'] == hostname
        assert resp['data']['resources'][1]['name'] == pool_name
        resp['data']['resources'][0]['name'] = 'localhost/docker'
        resp['data']['resources'][1]['name'] = 'localhost/docker Storage Pool'

    event_test(agent, 'docker/ping', post_func=post)
