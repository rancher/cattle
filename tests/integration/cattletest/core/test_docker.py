import re
import uuid as py_uuid
from common_fixtures import *  # NOQA

TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') == 'false'",
                               reason='DOCKER_TEST is not set')


@pytest.fixture(scope='session')
def docker_client(super_client):
    for host in super_client.list_host(state='active', remove_null=True,
                                       kind='docker'):
        key = super_client.create_api_key(accountId=host.accountId)
        super_client.wait_success(key)

        return api_client(key.publicValue, key.secretValue)

    raise Exception('Failed to find docker host, please register one')


@if_docker
def test_docker_create_only(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test',
                                               imageUuid=uuid,
                                               startOnCreate=False)
    try:
        container = docker_client.wait_success(container)

        assert container is not None
        assert 'container' == container.type
        image = super_client.reload(container).image()
        assert image.instanceKind == 'container'

        image_mapping = filter(
            lambda m: m.storagePool().external,
            image.imageStoragePoolMaps()
        )

        assert len(image_mapping) == 0

        assert not image.isPublic
        assert image.name == '{}'.format(image.data.dockerImage.fullName,
                                         image.data.dockerImage.id)
        assert image.name == TEST_IMAGE_LATEST
        assert image.data.dockerImage.repository == 'helloworld'
        assert image.data.dockerImage.namespace == 'ibuildthecloud'
        assert image.data.dockerImage.tag == 'latest'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_create_only_from_sha(docker_client, super_client):
    image_name = 'tianon/true@sha256:662fc60808e6d5628a090e39' \
                 'b4bcae694add28a626031cc889109c2cf2af5d73'
    uuid = 'docker:' + image_name
    container = docker_client.create_container(name='test-sha256',
                                               imageUuid=uuid,
                                               startOnCreate=False)
    try:
        container = docker_client.wait_success(container)

        assert container is not None
        assert 'container' == container.type
        image = super_client.reload(container).image()
        assert image.instanceKind == 'container'

        image_mapping = filter(
            lambda m: m.storagePool().external,
            image.imageStoragePoolMaps()
        )

        assert len(image_mapping) == 0

        assert not image.isPublic
        assert image.name == '{}'.format(image.data.dockerImage.fullName,
                                         image.data.dockerImage.id)
        assert image.name == image_name
        assert image.data.dockerImage.repository == 'true'
        assert image.data.dockerImage.namespace == 'tianon'
        assert image.data.dockerImage.tag == 'sha256:662fc60808e6d5628a090e' \
                                             '39b4bcae694add28a626031cc8891' \
                                             '09c2cf2af5d73'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_create_with_start(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test', imageUuid=uuid)

    try:
        assert container.state == 'creating'

        container = super_client.wait_success(container)

        assert container.state == 'running'

        assert container.data.dockerContainer.Image == TEST_IMAGE_LATEST

        assert len(container.volumes()) == 1

        image = container.volumes()[0].image()
        image = super_client.reload(image)
        image_mapping = filter(
            lambda m: not m.storagePool().external,
            image.imageStoragePoolMaps()
        )

        assert len(image_mapping) == 1
        assert image_mapping[0].imageId == image.id
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_build(docker_client, super_client):
    uuid = 'image-' + random_str()
    url = 'https://github.com/rancherio/tiny-build/raw/master/build.tar'
    container = docker_client.create_container(name='test',
                                               imageUuid='docker:' + uuid,
                                               build={
                                                   'context': url,
                                               })

    try:
        assert container.state == 'creating'
        container = super_client.wait_success(container)

        # This builds tianon/true which just dies
        assert container.state == 'running' or container.state == 'stopped'
        assert container.transitioning == 'no'
        assert container.data.dockerContainer.Image == uuid + ':latest'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_create_with_start_using_docker_io(docker_client, super_client):
    image = 'docker.io/' + TEST_IMAGE
    uuid = 'docker:' + image
    container = docker_client.create_container(name='test', imageUuid=uuid)
    container = super_client.wait_success(container)
    assert container.state == 'running'
    assert container.data.dockerContainer.Image == image + ':latest'
    if container is not None:
        docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_command(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test',
                                               imageUuid=uuid,
                                               command=['sleep', '42'])

    try:
        container = super_client.wait_success(container)
        assert container.data.dockerContainer.Command == 'sleep 42'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_command_args(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test',
                                               imageUuid=uuid,
                                               command=['sleep', '1', '2',
                                                        '3'])

    try:
        container = super_client.wait_success(container)
        assert container.data.dockerContainer.Command == 'sleep 1 2 3'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_short_lived_container(docker_client, super_client):
    container = docker_client.create_container(imageUuid="docker:tianon/true")
    container = wait_for_condition(
        docker_client, container,
        lambda x: x.state == 'stopped',
        lambda x: 'State is: ' + x.state)

    assert container.state == 'stopped'
    assert container.transitioning == 'no'


@if_docker
def test_docker_stop(docker_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = docker_client.wait_success(container)

    assert container.state == 'running'

    start = time.time()
    container = container.stop(timeout=0)
    assert container.state == 'stopping'

    container = docker_client.wait_success(container)
    delta = time.time() - start
    assert container.state == 'stopped'
    assert delta < 10


@if_docker
def test_docker_purge(docker_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test', imageUuid=uuid)

    assert container.state == 'creating'

    container = docker_client.wait_success(container)

    assert container.state == 'running'

    container = container.stop(timeout=0)
    assert container.state == 'stopping'

    container = docker_client.wait_success(container)
    assert container.state == 'stopped'

    docker_client.delete(container)

    container = docker_client.wait_success(container)
    assert container.state == 'removed'

    container = docker_client.wait_success(container.purge())
    assert container.state == 'purged'

    volume = container.volumes()[0]
    assert volume.state == 'removed'

    volume = docker_client.wait_success(volume.purge())
    assert volume.state == 'purged'


@if_docker
def test_docker_image_format(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(name='test', imageUuid=uuid)

    try:
        container = docker_client.wait_success(container)
        container = super_client.reload(container)

        assert container.image().format == 'docker'
        assert container.volumes()[0].image().format == 'docker'
        assert container.volumes()[0].format == 'docker'
    finally:
        if container is not None:
            docker_client.wait_success(docker_client.delete(container))


@if_docker
def test_docker_ports_from_container_publish_all(docker_client):
    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(networkMode='bridge',
                                       name='test',
                                       publishAllPorts=True,
                                       imageUuid=uuid)

    c = docker_client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports_link()
    assert len(ports) == 1
    port = ports[0]

    assert port.publicPort is not None
    assert port.privatePort == 8080
    assert port.publicIpAddressId is not None
    assert port.kind == 'imagePort'

    docker_client.wait_success(docker_client.delete(c))


@if_docker
def test_docker_ports_from_container_no_publish(docker_client):
    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(name='test',
                                       imageUuid=uuid)

    c = docker_client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports_link()
    assert len(ports) == 1
    port = ports[0]

    assert port.publicPort is None
    assert port.privatePort == 8080
    assert port.publicIpAddressId is not None
    assert port.kind == 'imagePort'

    docker_client.wait_success(docker_client.delete(c))


@if_docker
def test_docker_ports_from_container(docker_client, super_client):

    def reload(x):
        return super_client.reload(x)

    _ = reload

    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(networkMode='bridge',
                                       name='test',
                                       startOnCreate=False,
                                       publishAllPorts=True,
                                       imageUuid=uuid,
                                       ports=[
                                           '8081',
                                           '8082/tcp',
                                           '8083/udp'])

    c = docker_client.wait_success(c)
    assert c.state == 'stopped'

    count = 0
    for port in c.ports_link():
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

    c = docker_client.wait_success(c.start())
    assert c.state == 'running'

    network = super_client.reload(c).nics()[0].network()

    count = 0
    ip = None
    privateIp = None
    for port in c.ports_link():
        count += 1
        assert port.privateIpAddressId is not None
        privateIp = port.privateIpAddress()

        assert privateIp.kind == 'docker'
        assert privateIp.networkId == network.id
        assert privateIp.network() is not None
        assert _(privateIp).subnetId is None

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

    c = docker_client.wait_success(c.stop(timeout=0))
    assert c.state == 'stopped'

    count = 0
    for nic in _(c).nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'inactive'
            assert ip.address is None

    assert count == 1

    c = docker_client.wait_success(c.start())
    assert c.state == 'running'

    count = 0
    for nic in _(c).nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'active'
            assert ip.address is not None

    assert count == 1

    docker_client.wait_success(docker_client.delete(c))


@if_docker
def test_no_port_override(docker_client, super_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       ports=['8081:8080'])

    try:
        c = super_client.wait_success(c, timeout=240)

        assert c.state == 'running'
        ports = c.ports_link()

        assert len(ports) == 1
        assert ports[0].kind == 'userPort'
        assert ports[0].publicPort == 8081
        assert ports[0].privatePort == 8080
    finally:
        if c is not None:
            super_client.wait_success(super_client.delete(c))


@if_docker
def test_docker_volumes(docker_client, super_client):

    def reload(x):
        return super_client.reload(x)

    _ = reload

    uuid = TEST_IMAGE_UUID
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path
    baz_host_path = '/tmp/baz%s' % bind_mount_uuid
    baz_bind_mount = '%s:/baz:ro' % baz_host_path

    c = docker_client.create_container(name="volumes_test",
                                       imageUuid=uuid,
                                       startOnCreate=False,
                                       dataVolumes=['/foo',
                                                    bar_bind_mount,
                                                    baz_bind_mount])

    c = docker_client.wait_success(c)
    assert len(c.dataVolumes) == 3
    assert set(c.dataVolumes) == set(['/foo',
                                      bar_bind_mount,
                                      baz_bind_mount])

    c = super_client.wait_success(c.start())

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
    assert _(foo_vol).attachedState == 'inactive'

    assert bar_mount is not None
    assert bar_mount.permissions == 'rw'
    assert bar_vol is not None
    assert bar_vol.state == 'active'
    assert _(bar_vol).attachedState == 'inactive'

    assert baz_mount is not None
    assert baz_mount.permissions == 'ro'
    assert baz_vol is not None
    assert baz_vol.state == 'active'
    assert _(baz_vol).attachedState == 'inactive'

    assert not foo_vol.isHostPath

    assert bar_vol.isHostPath
    # We use 'in' instead of '==' because Docker uses the fully qualified
    # non-linked path and it might look something like: /mnt/sda1/<path>
    assert bar_host_path in bar_vol.uri

    assert baz_vol.isHostPath
    assert baz_host_path in baz_vol.uri

    c2 = docker_client.create_container(name="volumes_from_test",
                                        imageUuid=uuid,
                                        startOnCreate=False,
                                        dataVolumesFrom=[c.id])
    c2 = docker_client.wait_success(c2)
    assert len(c2.dataVolumesFrom) == 1
    assert set(c2.dataVolumesFrom) == set([c.id])

    c2 = super_client.wait_success(c2.start())
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

    c.stop(remove=True, timeout=0)
    c2.stop(remove=True, timeout=0)

    _check_path(foo_vol, True, docker_client, super_client)
    foo_vol = super_client.wait_success(foo_vol.deactivate())
    foo_vol = super_client.wait_success(foo_vol.remove())
    foo_vol = super_client.wait_success(foo_vol.purge())
    _check_path(foo_vol, False, docker_client, super_client)

    _check_path(bar_vol, True, docker_client, super_client)
    bar_vol = super_client.wait_success(bar_vol.deactivate())
    bar_vol = super_client.wait_success(bar_vol.remove())
    bar_vol = super_client.wait_success(bar_vol.purge())
    # Host bind mount. Wont actually delete the dir on the host.
    _check_path(bar_vol, True, docker_client, super_client)

    _check_path(baz_vol, True, docker_client, super_client)
    baz_vol = super_client.wait_success(baz_vol.deactivate())
    baz_vol = super_client.wait_success(baz_vol.remove())
    baz_vol = super_client.wait_success(baz_vol.purge())
    # Host bind mount. Wont actually delete the dir on the host.
    _check_path(baz_vol, True, docker_client, super_client)


@if_docker
def test_volumes_from_more_than_one_container(docker_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       dataVolumes=['/foo'])
    docker_client.wait_success(c)

    c2 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                        dataVolumes=['/bar'])
    docker_client.wait_success(c2)

    c3 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                        dataVolumesFrom=[c.id, c2.id])
    c3 = docker_client.wait_success(c3)

    mounts = c3.mounts()
    assert len(mounts) == 2
    paths = ['/foo', '/bar']
    for m in mounts:
        assert m.path in paths


@if_docker
def test_container_fields(docker_client, super_client):
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
    restart_policy = {"maximumRetryCount": 2, "name": "on-failure"}

    c = docker_client.create_container(name=test_name,
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
                                       command=["true"],
                                       entryPoint=["/bin/sh", "-c"],
                                       cpuShares=400,
                                       restartPolicy=restart_policy,
                                       devices="/dev/null:/dev/xnull:rw")

    c = super_client.wait_success(c)

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
def test_docker_mount_life_cycle(docker_client):
    uuid = TEST_IMAGE_UUID
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path

    c = docker_client.create_container(name="volumes_test",
                                       imageUuid=uuid,
                                       startOnCreate=False,
                                       dataVolumes=['/foo',
                                                    bar_bind_mount])

    c = docker_client.wait_success(c)
    c = docker_client.wait_success(c.start())

    def check_mounts(container, expected_state=None, length=0):
        mounts = container.mounts()
        assert len(mounts) == length
        if expected_state:
            for mount in mounts:
                assert mount.state == expected_state
        return mounts

    check_mounts(c, 'active', 2)

    c = docker_client.wait_success(c.stop(remove=True, timeout=0))
    check_mounts(c, 'inactive', 2)

    c = docker_client.wait_success(c.restore())
    assert c.state == 'stopped'
    check_mounts(c, 'inactive', 2)

    c = docker_client.wait_success(c.start())
    assert c.state == 'running'
    check_mounts(c, 'active', 2)

    c = docker_client.wait_success(c.stop(remove=True, timeout=0))
    c = docker_client.wait_success(c.purge())
    assert c.state == 'purged'
    check_mounts(c, 'removed', 2)


@if_docker
def test_docker_labels(docker_client):
    # 1.8 broke this behavior where labels would come from the images
    # one day maybe they will bring it back.
    # image_uuid = 'docker:ranchertest/labelled:v0.1.0'
    image_uuid = TEST_IMAGE_UUID

    c = docker_client.create_container(name="labels_test",
                                       imageUuid=image_uuid,
                                       labels={'io.rancher.testlabel.'
                                               'fromapi': 'yes'})
    c = docker_client.wait_success(c)

    def labels_callback():
        labels = c.instanceLabels()
        if len(labels) >= 3:
            return labels
        return None

    labels = wait_for(labels_callback)

    actual_labels = {}
    for l in labels:
        actual_labels[l.key] = l.value

    expected_labels = {
        # 'io.rancher.testlabel': 'value1',
        # 'io.rancher.testlabel.space': 'value 1',
        'io.rancher.testlabel.fromapi': 'yes',
        'io.rancher.container.uuid': c.uuid,
        'io.rancher.container.ip': c.primaryIpAddress + '/16',
    }
    assert actual_labels == expected_labels

    docker_client.wait_success(docker_client.delete(c))


@if_docker
def test_container_odd_fields(super_client, docker_client):
    c = docker_client.create_container(pidMode=None,
                                       imageUuid=TEST_IMAGE_UUID,
                                       logConfig={
                                           'driver': None,
                                           'config': None,
                                       })
    c = docker_client.wait_success(c)

    assert c.state == 'running'
    assert c.pidMode is None
    assert c.logConfig == {'driver': None, 'config': None}

    c = super_client.reload(c)

    assert c.data.dockerInspect.HostConfig.LogConfig['Type'] == 'json-file'
    assert not c.data.dockerInspect.HostConfig.LogConfig['Config']


@if_docker
def test_container_bad_build(super_client, docker_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       build={
                                           'context': None,
                                           'remote': None
                                       })
    c = docker_client.wait_success(c)

    assert c.state == 'running'
    assert c.pidMode is None
    assert c.build == {'context': None, 'remote': None}

    c = super_client.reload(c)

    assert c.data.dockerInspect.Config.Image == TEST_IMAGE_LATEST


@if_docker
def test_service_link_emu_docker_link(super_client, docker_client):
    env_name = random_str()
    env = docker_client.create_environment(name=env_name)
    env = docker_client.wait_success(env)
    assert env.state == "active"

    server = docker_client.create_service(name='server', launchConfig={
        'imageUuid': TEST_IMAGE_UUID
    }, environmentId=env.id)

    service = docker_client.create_service(name='client', launchConfig={
        'imageUuid': TEST_IMAGE_UUID
    }, environmentId=env.id)

    service_link = {"serviceId": server.id, "name": "other"}
    service.setservicelinks(serviceLinks=[service_link])
    server = docker_client.wait_success(server)
    service = docker_client.wait_success(service)

    server = docker_client.wait_success(server.activate())
    assert server.state == 'active'

    service = docker_client.wait_success(service.activate())
    assert service.state == 'active'

    instance = find_one(service.instances)
    instance = super_client.reload(instance)
    link = find_one(instance.instanceLinks)

    target_instance = find_one(server.instances)

    assert len(link.ports) == 1
    assert link.ports[0].privatePort == 8080
    assert link.ports[0].publicPort == 8080
    assert link.ports[0].protocol == 'tcp'
    assert link.ports[0].ipAddress is not None
    assert link.targetInstanceId == target_instance.id
    assert link.instanceNames == ['{}_server_1'.format(env_name)]

    docker_client.delete(env)


# This is really subideal to run this test not parallel but it is hard to
# not cause timeout issues with parallel tests.  Containers currently waiting
# on a config item change on the existing network agent have to timeout before
# they will try to update a different network agent.
@pytest.mark.nonparallel
@if_docker
def test_delete_network_agent(super_client, docker_client):
    # Create a container so we know the network agent is in use
    c1 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID)
    c1 = docker_client.wait_success(c1)
    assert c1.state == 'running'

    c1 = super_client.reload(c1)
    agentNsp = None
    for nsp in c1.nics()[0].network().networkServiceProviders():
        if nsp.type == 'agentInstanceProvider':
            agentNsp = nsp

    assert agentNsp is not None
    networkAgent = agentNsp.instances()[0]

    assert networkAgent.state == 'running'
    docker_client.delete(networkAgent)

    networkAgent = docker_client.wait_success(networkAgent)
    assert networkAgent.state == 'removed'

    c2 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID)
    c2 = docker_client.wait_success(c2, timeout=120)
    assert c2.state == 'running'


@if_docker
def test_service_links_with_no_ports(docker_client):
    env = docker_client.create_environment(name=random_str())
    env = docker_client.wait_success(env)
    assert env.state == "active"

    server = docker_client.create_service(name='server', launchConfig={
        'imageUuid': 'docker:busybox',
        'stdinOpen': True,
        'tty': True,
    }, environmentId=env.id)
    server = docker_client.wait_success(server)
    assert server.state == 'inactive'

    service = docker_client.create_service(name='client', launchConfig={
        'imageUuid': 'docker:busybox',
        'stdinOpen': True,
        'tty': True,
    }, environmentId=env.id)
    service = docker_client.wait_success(service)
    assert service.state == 'inactive'

    service_link = {"serviceId": server.id, "name": "bb"}
    service.setservicelinks(serviceLinks=[service_link])

    server = docker_client.wait_success(server.activate())
    assert server.state == 'active'
    service = docker_client.wait_success(service.activate())
    assert service.state == 'active'


def _check_path(volume, should_exist, client, super_client):
    path = _path_to_volume(volume)
    c = client. \
        create_container(name="volume_check",
                         imageUuid="docker:ranchertest/volume-test:v0.1.0",
                         networkMode=None,
                         environment={'TEST_PATH': path},
                         command='/opt/tools/check_path_exists.sh',
                         dataVolumes=[
                             '/var/lib/docker:/host/var/lib/docker',
                             '/tmp:/host/tmp'])

    c = super_client.wait_success(c)
    assert c.state == 'running'

    c = super_client.wait_success(c.stop())
    assert c.state == 'stopped'

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
