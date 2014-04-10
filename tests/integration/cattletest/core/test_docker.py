from common_fixtures import *  # NOQA
from cattle import ApiError

TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') != 'true'",
                               reason='DOCKER_TEST is not set')


@pytest.fixture(scope='module')
def docker_context(admin_client):
    return kind_context(admin_client, 'docker', external_pool=True)


@if_docker
def test_docker_image_create_vm(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test',
                                              imageUuid=uuid,
                                              startOnCreate=False)
    container = wait_success(admin_client, container)

    assert container.state == 'stopped'
    admin_client.delete(container)

    try:
        admin_client.create_virtual_machine(name='test',
                                            imageUuid=uuid)
    except ApiError, e:
        assert e.error.code == 'InvalidImageInstanceKind'


@if_docker
def test_docker_create_only(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test',
                                              imageUuid=uuid,
                                              startOnCreate=False)
    container = wait_success(admin_client, container)

    assert container is not None
    assert 'container' == container.type
    assert container.image().instanceKind == 'container'

    image = admin_client.list_image(uuid=uuid)[0]
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
def test_docker_create_with_start(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(admin_client, container)

    assert container.state == 'running'

    assert container.data.dockerContainer.Image == TEST_IMAGE_LATEST

    image = admin_client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: not m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context['pool'].id


@if_docker
def test_docker_command(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test',
                                              imageUuid=uuid,
                                              command='sleep 42')

    container = wait_success(admin_client, container)
    assert container.data.dockerContainer.Command == 'sleep 42'


@if_docker
def test_docker_command_args(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test',
                                              imageUuid=uuid,
                                              command='sleep',
                                              commandArgs=['1', '2', '3'])

    container = wait_success(admin_client, container)
    assert container.data.dockerContainer.Command == 'sleep 1 2 3'


@if_docker
def test_docker_stop(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(admin_client, container)

    assert container.state == 'running'

    container = container.stop()
    assert container.state == 'stopping'

    container = wait_success(admin_client, container)
    assert container.state == 'stopped'


@if_docker
def test_docker_purge(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(admin_client, container)

    assert container.state == 'running'

    container = container.stop()
    assert container.state == 'stopping'

    container = wait_success(admin_client, container)
    assert container.state == 'stopped'

    admin_client.delete(container)

    container = wait_success(admin_client, container)
    assert container.state == 'removed'

    container = wait_success(admin_client, container.purge())
    assert container.state == 'purged'

    volume = container.volumes()[0]
    assert volume.state == 'removed'

    volume = wait_success(admin_client, volume.purge())
    assert volume.state == 'purged'


@if_docker
def test_docker_image_format(admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    container = wait_success(admin_client, container)

    assert container.image().format == 'docker'
    assert container.volumes()[0].image().format == 'docker'
    assert container.volumes()[0].format == 'docker'
