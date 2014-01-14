from common_fixtures import *
import os

TEST_IMAGE = "ibuildthecloud/helloworld"
TEST_IMAGE_UUID = "docker:" + TEST_IMAGE

DOCKER_HOST = "ssh://docker:docker@localhost"

if_docker = pytest.mark.skipif('os.environ.get("DOCKER_TEST") is None', reason="DOCKER_TEST is not set")


@pytest.fixture(scope="module")
def docker_host(admin_client, docker_agent):
    return create_type_by_uuid(admin_client, "host", "dockerhost1", kind="docker", agentId=docker_agent.id)


@pytest.fixture(scope="module")
def docker_external_pool(admin_client):
    return create_type_by_uuid(admin_client, "storagePool", "dockerexternalpool", kind="docker", external=True)


@pytest.fixture(scope="module")
def docker_pool(admin_client, docker_host, docker_agent):
    pool = create_type_by_uuid(admin_client, "storagePool", "dockerpool1", kind="docker", agentId=docker_agent.id)
    assert not pool.external

    create_type_by_uuid(admin_client, "storagePoolHostMap", "dockerpool1-dockerhost",
                        storagePoolId=pool.id, hostId=docker_host.id)

    return pool


@pytest.fixture(scope="module")
def docker_agent(admin_client):
    return create_type_by_uuid(admin_client, "agent", "dockeragent1", kind="docker", uri=DOCKER_HOST)


@pytest.fixture(scope="module")
def docker_context(docker_host, docker_pool, docker_external_pool, docker_agent):
    return {
        "host": docker_host,
        "pool": docker_pool,
        "external_pool": docker_external_pool,
        "agent": docker_agent
    }


@if_docker
def test_docker_create_only(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name="test", imageUuid=uuid, startOnCreate=False)
    container = wait_success(client, container)

    assert container is not None
    assert "container" == container.type

    image = client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context["external_pool"].id

    assert image.isPublic
    assert image.uuid == uuid
    assert image.data.dockerImage.repository == "helloworld"
    assert image.data.dockerImage.namespace == "ibuildthecloud"
    assert image.data.dockerImage.tag == "latest"
    assert image.data.dockerImage.id is not None

    return container


@if_docker
def test_docker_create_with_start(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name="test", imageUuid=uuid)

    assert container.state == "creating"

    container = wait_success(client, container)

    assert container.state == "running"

    image = client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: not m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context["pool"].id
