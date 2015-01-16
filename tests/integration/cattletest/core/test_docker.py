import re
import time
import uuid as py_uuid
from common_fixtures import *  # NOQA
from cattle import ApiError

TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') == 'false'",
                               reason='DOCKER_TEST is not set')


@pytest.fixture(scope='session')
def docker_context(internal_test_client):
    for host in internal_test_client.list_host(state='active',
                                               remove_null=True,
                                               kind='docker'):
        return kind_context(internal_test_client, 'docker', external_pool=True,
                            agent=host.agent())

    raise Exception('Failed to find docker host, please register one')


@if_docker
def test_docker_image_create_vm(admin_client, internal_test_client,
                                docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test',
                                              imageUuid=uuid,
                                              startOnCreate=False)
    container = wait_success(admin_client, container)

    assert container.state == 'stopped'
    admin_client.delete(container)

    try:
        internal_test_client.create_virtual_machine(name='test',
                                                    imageUuid=uuid)
    except ApiError, e:
        assert e.error.code == 'InvalidImageInstanceKind'


@if_docker
def test_docker_create_only(admin_client, internal_test_client,
                            docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid,
                                              startOnCreate=False)
    container = wait_success(admin_client, container)

    assert container is not None
    assert 'container' == container.type
    container = internal_test_client.reload(container)
    assert container.image().instanceKind == 'container'

    image = admin_client.list_image(uuid=uuid)[0]
    assert image.isPublic
    assert image.name == '{}'.format(image.data.dockerImage.fullName,
                                     image.data.dockerImage.id)
    assert image.uuid == uuid
    assert image.data.dockerImage.repository == 'helloworld'
    assert image.data.dockerImage.namespace == 'ibuildthecloud'
    assert image.data.dockerImage.tag == 'latest'
    assert image.data.dockerImage.id is not None

    image = internal_test_client.list_image(uuid=uuid)[0]
    image_mapping = filter(
        lambda m: m.storagePool().external,
        image.imageStoragePoolMaps()
    )

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == docker_context['external_pool'].id

    return container


@if_docker
def test_docker_create_with_start(admin_client, internal_test_client,
                                  docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = wait_success(admin_client, container)

    assert container.state == 'running'

    assert container.data.dockerContainer.Image == TEST_IMAGE_LATEST

    image = admin_client.list_image(uuid=uuid)[0]
    image = internal_test_client.reload(image)
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
def test_docker_image_format(admin_client, internal_test_client,
                             docker_context):
    uuid = TEST_IMAGE_UUID
    container = admin_client.create_container(name='test', imageUuid=uuid)

    container = wait_success(admin_client, container)
    container = internal_test_client.reload(container)
    assert container.image().format == 'docker'
    assert container.volumes()[0].image().format == 'docker'
    assert container.volumes()[0].format == 'docker'


@if_docker
def test_docker_ports_from_container_publish_all(client, admin_client,
                                                 docker_context):
    uuid = TEST_IMAGE_UUID
    c = client.create_container(name='test',
                                publishAllPorts=True,
                                imageUuid=uuid)

    c = client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports()
    assert len(ports) == 1
    port = ports[0]

    assert port.publicPort is not None
    assert port.privatePort == 8080
    assert port.publicIpAddressId is not None
    assert port.kind == 'imagePort'

    client.delete(c)


@if_docker
def test_docker_ports_from_container_no_publish(client, admin_client,
                                                docker_context):
    uuid = TEST_IMAGE_UUID
    c = client.create_container(name='test',
                                imageUuid=uuid)

    c = client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports()
    assert len(ports) == 1
    port = ports[0]

    assert port.publicPort is None
    assert port.privatePort == 8080
    assert port.publicIpAddressId is not None
    assert port.kind == 'imagePort'

    client.delete(c)


@if_docker
def test_docker_ports_from_container(client, admin_client,
                                     internal_test_client, docker_context):
    network = internal_test_client.create_network(isPublic=True)
    network = internal_test_client.wait_success(network)
    uuid = TEST_IMAGE_UUID
    c = client.create_container(name='test',
                                startOnCreate=False,
                                publishAllPorts=True,
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
    c = internal_test_client.reload(c)
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
    c = internal_test_client.reload(c)
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
    c = internal_test_client.reload(c)
    for nic in c.nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'active'
            assert ip.address is not None

    assert count == 1

    c.stop(remove=True)


@if_docker
def test_agent_instance(internal_test_client, docker_context):
    network = create_and_activate(internal_test_client, 'hostOnlyNetwork',
                                  hostVnetUri='bridge://docker0',
                                  dynamicCreateVnet=True)

    ni = create_and_activate(internal_test_client, 'agentInstanceProvider',
                             networkId=network.id)

    create_and_activate(internal_test_client, 'dnsService',
                        networkId=network.id,
                        networkServiceProviderId=ni.id)

    c = internal_test_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                              networkIds=[network.id])
    # TODO: Figure out whats failing here
    c = internal_test_client.wait_success(c, timeout=240)
    assert c.state == 'running'

    agent_instance = None
    for nic in network.nics():
        instance = nic.instance()
        if instance.agentId is not None:
            agent_instance = instance
            break

    assert agent_instance is not None

    agent_instance = internal_test_client.wait_success(agent_instance)
    assert agent_instance.state == 'running'

    agent = internal_test_client.wait_success(agent_instance.agent())
    assert agent.state == 'active'


@if_docker
def test_no_port_override(internal_test_client, docker_context):
    network = find_one(internal_test_client.list_network,
                       uuid='managed-docker0')

    c = internal_test_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                              networkIds=[network.id],
                                              ports=['8081:8080'])

    # TODO: Figure out why this takes so long
    c = internal_test_client.wait_success(c, timeout=240)

    assert c.state == 'running'
    ports = c.ports()

    assert len(ports) == 1
    assert ports[0].kind == 'userPort'
    assert ports[0].publicPort == 8081
    assert ports[0].privatePort == 8080


@if_docker
def test_docker_volumes(client, admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path
    baz_host_path = '/tmp/baz%s' % bind_mount_uuid
    baz_bind_mount = '%s:/baz:ro' % baz_host_path

    c = admin_client.create_container(name="volumes_test",
                                      imageUuid=uuid,
                                      startOnCreate=False,
                                      dataVolumes=['/foo',
                                                   bar_bind_mount,
                                                   baz_bind_mount])

    c = admin_client.wait_success(c)
    assert len(c.dataVolumes) == 3
    assert set(c.dataVolumes) == set(['/foo',
                                      bar_bind_mount,
                                      baz_bind_mount])

    c = admin_client.wait_success(c.start())

    volumes = c.volumes()
    assert len(volumes) == 1

    mounts = c.mounts()
    assert len(mounts) == 3
    foo_mount, bar_mount, baz_mount = None, None, None
    foo_vol, bar_vol, baz_vol = None, None, None
    for mount in mounts:
        assert mount.instance().id == c.id
        if mount.path == '/foo':
            foo_mount = mount
            foo_vol = mount.volume()
        elif mount.path == '/bar':
            bar_mount = mount
            bar_vol = mount.volume()
        elif mount.path == '/baz':
            baz_mount = mount
            baz_vol = mount.volume()

    assert foo_mount is not None
    assert foo_mount.permissions == 'rw'
    assert foo_vol is not None
    assert foo_vol.state == 'active'
    assert foo_vol.attachedState == 'inactive'

    assert bar_mount is not None
    assert bar_mount.permissions == 'rw'
    assert bar_vol is not None
    assert bar_vol.state == 'active'
    assert bar_vol.attachedState == 'inactive'

    assert baz_mount is not None
    assert baz_mount.permissions == 'ro'
    assert baz_vol is not None
    assert baz_vol.state == 'active'
    assert baz_vol.attachedState == 'inactive'

    assert not foo_vol.isHostPath

    assert bar_vol.isHostPath
    # We use 'in' instead of '==' because Docker uses the fully qualified
    # non-linked path and it might look something like: /mnt/sda1/<path>
    assert bar_host_path in bar_vol.uri

    assert baz_vol.isHostPath
    assert baz_host_path in baz_vol.uri

    c2 = admin_client.create_container(name="volumes_from_test",
                                       imageUuid=uuid,
                                       startOnCreate=False,
                                       dataVolumesFrom=[c.id])
    c2 = admin_client.wait_success(c2)
    assert len(c2.dataVolumesFrom) == 1
    assert set(c2.dataVolumesFrom) == set([c.id])

    c2 = admin_client.wait_success(c2.start())
    c2_mounts = c2.mounts()
    assert len(c2_mounts) == 3

    for mount in c2_mounts:
        assert mount.instance().id == c2.id
        if mount.path == '/foo':
            assert mount.volumeId == foo_vol.id
        elif mount.path == '/bar':
            assert mount.volumeId == bar_vol.id
        elif mount.path == '/baz':
            assert mount.volumeId == baz_vol.id

    c.stop(remove=True)
    c2.stop(remove=True)

    _check_path(foo_vol, True, admin_client)
    foo_vol = admin_client.wait_success(foo_vol.deactivate())
    foo_vol = admin_client.wait_success(foo_vol.remove())
    foo_vol = admin_client.wait_success(foo_vol.purge())
    _check_path(foo_vol, False, admin_client)

    _check_path(bar_vol, True, admin_client)
    bar_vol = admin_client.wait_success(bar_vol.deactivate())
    bar_vol = admin_client.wait_success(bar_vol.remove())
    bar_vol = admin_client.wait_success(bar_vol.purge())
    # Host bind mount. Wont actually delete the dir on the host.
    _check_path(bar_vol, True, admin_client)

    _check_path(baz_vol, True, admin_client)
    baz_vol = admin_client.wait_success(baz_vol.deactivate())
    baz_vol = admin_client.wait_success(baz_vol.remove())
    baz_vol = admin_client.wait_success(baz_vol.purge())
    # Host bind mount. Wont actually delete the dir on the host.
    _check_path(baz_vol, True, admin_client)


@if_docker
def test_container_fields(client, admin_client, docker_context):
    caps = ["SYS_MODULE", "SYS_RAWIO", "SYS_PACCT", "SYS_ADMIN",
            "SYS_NICE", "SYS_RESOURCE", "SYS_TIME", "SYS_TTY_CONFIG",
            "MKNOD", "AUDIT_WRITE", "AUDIT_CONTROL", "MAC_OVERRIDE",
            "MAC_ADMIN", "NET_ADMIN", "SYSLOG", "CHOWN", "NET_RAW",
            "DAC_OVERRIDE", "FOWNER", "DAC_READ_SEARCH", "FSETID",
            "KILL", "SETGID", "SETUID", "LINUX_IMMUTABLE",
            "NET_BIND_SERVICE", "NET_BROADCAST", "IPC_LOCK",
            "IPC_OWNER", "SYS_CHROOT", "SYS_PTRACE", "SYS_BOOT",
            "LEASE", "SETFCAP", "WAKE_ALARM", "BLOCK_SUSPEND", "ALL"]
    test_name = 'container_test'
    image_uuid = 'docker:ibuildthecloud/helloworld'
    expectedLxcConf = {"lxc.network.type": "veth"}
    restart_policy = {"maximumRetryCount": 2, "name": "on-failure"}

    c = admin_client.create_container(name=test_name,
                                      imageUuid=image_uuid,
                                      capAdd=caps,
                                      capDrop=caps,
                                      dnsSearch=['8.8.8.8', '1.2.3.4'],
                                      dns=['8.8.8.8', '1.2.3.4'],
                                      privileged=True,
                                      domainName="rancher.io",
                                      memory=8000000,
                                      memorySwap=16000000,
                                      cpuSet="0,1",
                                      stdinOpen=True,
                                      tty=True,
                                      entryPoint=["/bin/sh", "-c"],
                                      lxcConf=expectedLxcConf,
                                      cpuShares=400,
                                      restartPolicy=restart_policy,
                                      devices="/dev/null:/dev/xnull:rw")

    c = admin_client.wait_success(c)

    assert set(c.data['dockerInspect']['HostConfig']['CapAdd']) == set(caps)
    assert set(c.data['dockerInspect']['HostConfig']['CapDrop']) == set(caps)
    actual_dns = c.data['dockerInspect']['HostConfig']['Dns']
    assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4'])
    actual_dns = c.data['dockerInspect']['HostConfig']['DnsSearch']
    assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4'])
    assert c.data['dockerInspect']['HostConfig']['Privileged']
    assert c.data['dockerInspect']['Config']['Domainname'] == "rancher.io"
    assert c.data['dockerInspect']['Config']['Memory'] == 8000000
    assert c.data['dockerInspect']['Config']['MemorySwap'] == 16000000
    assert c.data['dockerInspect']['Config']['Cpuset'] == "0,1"
    assert c.data['dockerInspect']['Config']['Tty']
    assert c.data['dockerInspect']['Config']['OpenStdin']
    actual_entry_point = set(c.data['dockerInspect']['Config']['Entrypoint'])
    assert actual_entry_point == set(["/bin/sh", "-c"])
    for conf in c.data['dockerInspect']['HostConfig']['LxcConf']:
        assert expectedLxcConf[conf['Key']] == conf['Value']
    assert c.data['dockerInspect']['Config']['CpuShares'] == 400
    act_restart_pol = c.data['dockerInspect']['HostConfig']['RestartPolicy']
    assert act_restart_pol['MaximumRetryCount'] == 2
    assert act_restart_pol['Name'] == "on-failure"
    actual_devices = c.data['dockerInspect']['HostConfig']['Devices']
    assert len(actual_devices) == 1
    assert actual_devices[0]['CgroupPermissions'] == "rw"
    assert actual_devices[0]['PathOnHost'] == "/dev/null"
    assert actual_devices[0]['PathInContainer'] == "/dev/xnull"


@if_docker
def test_docker_mount_life_cycle(client, admin_client, docker_context):
    uuid = TEST_IMAGE_UUID
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path

    c = admin_client.create_container(name="volumes_test",
                                      imageUuid=uuid,
                                      startOnCreate=False,
                                      dataVolumes=['/foo', bar_bind_mount])

    c = admin_client.wait_success(c)
    c = admin_client.wait_success(c.start())

    def check_mounts(container, expected_state=None, length=0):
        mounts = container.mounts()
        assert len(mounts) == length
        if expected_state:
            for mount in mounts:
                assert mount.state == expected_state
        return mounts

    check_mounts(c, 'active', 2)

    c = admin_client.wait_success(c.stop(remove=True))
    check_mounts(c, 'inactive', 2)

    c = admin_client.wait_success(c.restore())
    assert c.state == 'stopped'
    check_mounts(c, 'inactive', 2)

    c = admin_client.wait_success(c.start())
    assert c.state == 'running'
    check_mounts(c, 'active', 2)

    c = admin_client.wait_success(c.stop(remove=True))
    c = admin_client.wait_success(c.purge())
    assert c.state == 'purged'
    check_mounts(c, 'removed', 2)


def _check_path(volume, should_exist, internal_test_client):
    path = _path_to_volume(volume)
    c = internal_test_client. \
        create_container(name="volume_check",
                         imageUuid="docker:cjellick/rancher-test-tools",
                         startOnCreate=False,
                         environment={'TEST_PATH': path},
                         command='/opt/tools/check_path_exists.sh',
                         dataVolumes=[
                             '/var/lib/docker:/host/var/lib/docker',
                             '/tmp:/host/tmp'])
    c.start()
    c = internal_test_client.wait_success(c)
    c = _wait_until_stopped(c, internal_test_client)

    code = c.data.dockerInspect.State.ExitCode
    if should_exist:
        # The exit code of the container should be a 10 if the path existed
        assert code == 10
    else:
        # And 11 if the path did not exist
        assert code == 11

    c.remove()


def _path_to_volume(volume):
    path = volume.uri.replace('file://', '')
    mounted_path = re.sub('^.*?/var/lib/docker', '/host/var/lib/docker',
                          path)
    if not mounted_path.startswith('/host/var/lib/docker'):
        mounted_path = re.sub('^.*?/tmp', '/host/tmp',
                              path)
    return mounted_path


def _wait_until_stopped(container, admin_client, timeout=45):
        start = time.time()
        container = admin_client.reload(container)
        while container.state != 'stopped':
            time.sleep(.5)
            container = admin_client.reload(container)
            if time.time() - start > timeout:
                raise Exception('Timeout waiting for container to stop.')

        return container
