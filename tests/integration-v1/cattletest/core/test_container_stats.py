from common import *  # NOQA
from test_docker import docker_client, TEST_IMAGE_UUID, if_docker

import jwt

docker_client


@if_docker
def test_stats_container(docker_client, cattle_url):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')
    container = docker_client.wait_success(container)

    assert len(container.hosts()) == 1

    stats_access = container.containerStats()

    assert stats_access.token.index('.') > 0
    assert '/v1/containerstats/' in stats_access.url
    try:
        payload = jwt.decode(stats_access.token, verify=False)
        assert 'containerIds' in payload
        containerIds = payload['containerIds']
        assert len(containerIds) == 1
    except jwt.InvalidTokenError:
        assert False


@if_docker
def test_stats_service(docker_client, context, cattle_url):
    env = docker_client.create_environment(name=random_str())
    env = docker_client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = docker_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           launchConfig=launch_config)
    service = docker_client.wait_success(service)
    assert service.state == "inactive"
    stats_access = service.containerStats()

    try:
        payload = jwt.decode(stats_access.token, verify=False)

        assert 'service' in payload
    except jwt.InvalidTokenError:
        assert False
