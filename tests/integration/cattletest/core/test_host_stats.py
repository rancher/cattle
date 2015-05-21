from common_fixtures import *  # NOQA
from test_docker import docker_client, TEST_IMAGE_UUID, if_docker

import requests

# work around flake8 issue
docker_client


@if_docker
def test_stats_host(docker_client, context):
    host = docker_client.list_host()[0]
    sim_host = context.host

    assert 'stats' in host.links
    assert 'stats' not in sim_host.links

    found_ip = _get_host_stats_ip(host)
    stats_access = host.stats()

    assert stats_access.token.index('.') > 0
    assert stats_access.url == 'ws://%s:9345/v1/stats' % found_ip.address


def _get_host_stats_ip(host):
    found_ip = None
    for ip in host.ipAddresses():
        if found_ip is None:
            found_ip = ip
        elif found_ip.role == 'primary':
            found_ip = ip
            break
        elif ip.createdTS < found_ip.createdTS:
            found_ip = ip

    assert found_ip is not None
    assert found_ip.address is not None

    return found_ip


@if_docker
def test_stats_container(docker_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test', imageUuid=uuid)
    container = docker_client.wait_success(container)

    assert container.state == 'running'
    assert len(container.hosts()) == 1

    host = container.hosts()[0]

    found_ip = _get_host_stats_ip(host)
    stats_access = container.stats()

    assert stats_access.token.index('.') > 0
    assert stats_access.url == 'ws://{}:9345/v1/stats/{}'.format(
        found_ip.address, container.externalId)


def test_host_api_key_download(client):
    url = client._url.split('v1', 1)[0] + 'v1/scripts/api.crt'
    assert url is not None

    cert = requests.get(url).text

    assert cert is not None
    assert cert.startswith('-----BEGIN PUBLIC KEY-')
