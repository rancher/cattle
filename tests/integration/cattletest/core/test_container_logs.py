from common_fixtures import *  # NOQA
from test_docker import docker_context, TEST_IMAGE_UUID, if_docker

docker_context


def _get_container_logs_ip(host):
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
def test_logs_container(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)
    container = admin_client.wait_success(container)

    assert len(container.hosts()) == 1

    host = container.hosts()[0]

    found_ip = _get_container_logs_ip(host)
    logs_access = container.logs()

    assert logs_access.token.index('.') > 0
    assert logs_access.url == \
        'ws://{}:9345/v1/logs/'.format(found_ip.address)
