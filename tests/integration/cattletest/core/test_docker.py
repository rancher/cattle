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


@if_docker
def test_docker_ports_from_container(client, admin_client, docker_context):
    network = admin_client.create_network(isPublic=True)
    network = admin_client.wait_success(network)

    uuid = TEST_IMAGE_UUID
    c = client.create_container(name='test',
                                startOnCreate=False,
                                networkIds=[network.id],
                                imageUuid=uuid,
                                ports=[
                                    '8081',
                                    '8082/tcp',
                                    '8083/udp'])

    c = client.wait_success(c)
    assert c.state == 'stopped'

    count = 0
    for port in c.ports():
        count += 1
        assert port.kind == 'userPort'
        assert port.publicPort is None
        assert port.privateIpAddressId is None
        assert port.publicIpAddressId is None

        if port.privatePort == 8081:
            assert port.protocol == 'tcp'
        elif port.privatePort == 8082:
            assert port.protocol == 'tcp'
        elif port.privatePort == 8083:
            assert port.protocol == 'udp'
        else:
            assert False

    assert count == 3

    c = client.wait_success(c.start())
    assert c.state == 'running'

    count = 0
    ip = None
    privateIp = None
    for port in c.ports():
        count += 1
        assert port.privateIpAddressId is not None
        privateIp = port.privateIpAddress()

        assert privateIp.kind == 'docker'
        assert privateIp.networkId == network.id
        assert privateIp.network() is not None
        assert privateIp.subnetId is None

        assert port.publicPort is not None
        assert port.publicIpAddressId is not None

        if ip is None:
            ip = port.publicIpAddressId
        assert port.publicIpAddressId == ip

        if port.privatePort == 8081:
            assert port.kind == 'userPort'
            assert port.protocol == 'tcp'
        elif port.privatePort == 8082:
            assert port.kind == 'userPort'
            assert port.protocol == 'tcp'
        elif port.privatePort == 8083:
            assert port.kind == 'userPort'
            assert port.protocol == 'udp'
        elif port.privatePort == 8080:
            assert port.kind == 'imagePort'
        else:
            assert False

    assert count == 4

    assert c.primaryIpAddress == privateIp.address

    c = client.wait_success(c.stop())
    assert c.state == 'stopped'

    count = 0
    for nic in c.nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'inactive'
            assert ip.address is None

    assert count == 1

    c = client.wait_success(c.start())
    assert c.state == 'running'

    count = 0
    for nic in c.nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'active'
            assert ip.address is not None

    assert count == 1

    c.stop(remove=True)


@if_docker
def test_agent_instance(admin_client, docker_context):
    network = create_and_activate(admin_client, 'hostOnlyNetwork',
                                  hostVnetUri='bridge://docker0',
                                  dynamicCreateVnet=True)

    create_and_activate(admin_client, 'ipsecHostNatService',
                        networkId=network.id)

    c = admin_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                      networkIds=[network.id])
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    agent_instance = None
    for nic in network.nics():
        instance = nic.instance()
        if instance.agentId is not None:
            agent_instance = instance
            break

    assert agent_instance is not None

    agent_instance = admin_client.wait_success(agent_instance)
    assert agent_instance.state == 'running'

    agent = admin_client.wait_success(agent_instance.agent())
    assert agent.state == 'active'
