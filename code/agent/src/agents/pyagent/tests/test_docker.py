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

    test_instance_activate(agent, responses)


@if_docker
def test_instance_activate(agent, responses):
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

    test_instance_activate(agent, responses)

    def post(req, resp):
        del resp['data']['instance']['+data']['dockerContainer']['Created']
        del resp['data']['instance']['+data']['dockerContainer']['Id']
        del resp['data']['instance']['+data']['dockerContainer']['Status']
        del resp['data']['instance']['+data']['+fields']['dockerIp']

    event_test(agent, 'docker/instance_deactivate', post_func=post)


@if_docker
def test_ping(agent, responses):
    CONFIG_OVERRIDE['DOCKER_UUID'] = 'testuuid'
    CONFIG_OVERRIDE['PHYSICAL_HOST_UUID'] = 'hostuuid'

    def post(req, resp):
        hostname = Config.hostname() + '/docker'
        pool_name = hostname + ' Storage Pool'
        resources = resp['data']['resources']
        resources = filter(lambda x: x['kind'] == 'docker', resources)
        resp['data']['resources'] = resources

        assert resp['data']['resources'][0]['name'] == hostname
        assert resp['data']['resources'][1]['name'] == pool_name
        resp['data']['resources'][0]['name'] = 'localhost/docker'
        resp['data']['resources'][1]['name'] = 'localhost/docker Storage Pool'

    event_test(agent, 'docker/ping', post_func=post)
