from common_fixtures import *
import os

DOCKER_POOL_UUID = "dockerexternalpool"

ifdocker = pytest.mark.skipif('os.environ.get("DOCKER_TEST") is None', reason="DOCKER_TEST is not set")

@pytest.fixture(scope="module")
def docker_pool(admin_client):
    docker_pools = admin_client.list_storagePool(uuid=DOCKER_POOL_UUID)
    pool = None
    if len(docker_pools) == 0:
        pool = admin_client.create_storagePool(uuid=DOCKER_POOL_UUID, kind="docker", external=True)
    else:
        pool = docker_pools[0]

    pool = wait_success(admin_client, pool)
    if pool.state == "inactive":
        pool.activate()
        pool = wait_success(admin_client, pool)

    assert pool.state == "active"
    assert pool.kind == "docker"
    assert pool.external

    return pool


@ifdocker
def test_docker_create(client, docker_pool):
    uuid = "docker:ibuildthecloud/hello-world"
    container = client.create_container(name="test", imageUuid=uuid)
    container = wait_success(client, container)

    assert container is not None
    assert "container" == container.type

    image = client.list_image(uuid=uuid)[0]
    image_mapping = image.imageStoragePoolMaps()

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_pool.id

    assert image.isPublic
    assert image.uuid == uuid
    assert image.data.dockerImage.repository == "hello-world"
    assert image.data.dockerImage.namespace == "ibuildthecloud"
    assert image.data.dockerImage.tag == "latest"
    assert image.data.dockerImage.id is not None
