from common_fixtures import *  # NOQA
import os

TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

DOCKER_HOST = os.getenv('DOCKER_HOST', 'ssh://root@localhost:22')

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') != 'true'",
                               reason='DOCKER_TEST is not set')


@pytest.fixture(scope='module')
def docker_host(admin_client, docker_agent):
    hosts = docker_agent.hosts()
    assert len(hosts) == 1
    return activate_resource(admin_client, hosts[0])


@pytest.fixture(scope='module')
def docker_external_pool(admin_client):
    pools = admin_client.list_storagePool(kind='docker', external=True)
    assert len(pools) == 1
    return activate_resource(admin_client, pools[0])


@pytest.fixture(scope='module')
def docker_pool(admin_client, docker_host, docker_agent):
    pools = docker_agent.storagePools()
    assert len(pools) == 1
    return activate_resource(admin_client, pools[0])


@pytest.fixture(scope='module')
def docker_agent(admin_client):
    return create_type_by_uuid(admin_client, 'agent', 'dockeragent1',
                               kind='docker', uri=DOCKER_HOST)


@pytest.fixture(scope='module')
def docker_context(docker_host, docker_pool, docker_external_pool,
                   docker_agent):
    return {
        'host': docker_host,
        'pool': docker_pool,
        'external_pool': docker_external_pool,
        'agent': docker_agent
    }


@if_docker
def test_docker_create_only(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test', imageUuid=uuid,
                                        startOnCreate=False)
    container = wait_success(client, container)

    assert container is not None
    assert 'container' == container.type

    image = client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context['external_pool'].id

    assert image.isPublic
    assert image.name == '{} ({})'.format(image.data.dockerImage.fullName,
                                          image.data.dockerImage.id)
    assert image.uuid == uuid
    assert image.data.dockerImage.repository == 'helloworld'
    assert image.data.dockerImage.namespace == 'ibuildthecloud'
    assert image.data.dockerImage.tag == 'latest'
    assert image.data.dockerImage.id is not None

    return container


@if_docker
def test_docker_create_with_start(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(client, container)

    assert container.state == 'running'

    assert container.data.dockerContainer.Image == TEST_IMAGE_LATEST

    image = client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: not m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context['pool'].id


@if_docker
def test_docker_command(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test',
                                        imageUuid=uuid,
                                        command='sleep 42')

    container = wait_success(client, container)
    assert container.data.dockerContainer.Command == 'sleep 42'


@if_docker
def test_docker_command_args(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test',
                                        imageUuid=uuid,
                                        command='sleep',
                                        commandArgs=['1', '2', '3'])

    container = wait_success(client, container)
    assert container.data.dockerContainer.Command == 'sleep 1 2 3'


@if_docker
def test_docker_stop(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(client, container)

    assert container.state == 'running'

    container = container.stop()
    assert container.state == 'stopping'

    container = wait_success(client, container)
    assert container.state == 'stopped'


@if_docker
def test_docker_purge(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(client, container)

    assert container.state == 'running'

    container = container.stop()
    assert container.state == 'stopping'

    container = wait_success(client, container)
    assert container.state == 'stopped'

    client.delete(container)

    container = wait_success(client, container)
    assert container.state == 'removed'

    container = wait_success(client, container.purge())
    assert container.state == 'purged'

    volume = container.volumes()[0]
    assert volume.state == 'removed'

    volume = wait_success(client, volume.purge())
    assert volume.state == 'purged'


@if_docker
def test_docker_image_format(client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test', imageUuid=uuid)

    container = wait_success(client, container)

    assert container.image().format == 'docker'
    assert container.volumes()[0].image().format == 'docker'
    assert container.volumes()[0].format == 'docker'
