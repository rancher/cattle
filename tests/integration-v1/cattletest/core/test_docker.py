import re
import uuid as py_uuid
from common import *  # NOQA
from test_volume import VOLUME_CLEANUP_LABEL

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

        wait_for(lambda: host.agent().state == 'active')
        wait_for(lambda: len(host.storagePools()) > 0 and
                 host.storagePools()[0].state == 'active')
        return api_client(key.publicValue, key.secretValue)

    raise Exception('Failed to find docker host, please register one')


@if_docker
def test_docker_create_only(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge',
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
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_docker_create_only_from_sha(docker_client, super_client):
    image_name = 'tianon/true@sha256:662fc60808e6d5628a090e39' \
                 'b4bcae694add28a626031cc889109c2cf2af5d73'
    uuid = 'docker:' + image_name
    container = docker_client.create_container(name='test-sha256',
                                               imageUuid=uuid,
                                               networkMode='bridge',
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
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_docker_create_with_start(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')

    try:
        assert container.state == 'creating'

        container = super_client.wait_success(container)

        assert container.state == 'running'

        assert container.data.dockerContainer.Image == TEST_IMAGE

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
            docker_client.delete(container)


@if_docker
def test_docker_build(docker_client, super_client):
    uuid = 'image-' + random_str()
    url = 'https://github.com/rancherio/tiny-build/raw/master/build.tar'
    container = docker_client.create_container(imageUuid='docker:' + uuid,
                                               networkMode='bridge',
                                               build={
                                                   'context': url,
                                               })

    try:
        assert container.state == 'creating'
        container = super_client.wait_success(container)

        # This builds tianon/true which just dies
        assert container.state == 'running' or container.state == 'stopped'
        assert container.transitioning == 'no'
        assert container.data.dockerContainer.Image == uuid
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_docker_create_with_start_using_docker_io(docker_client, super_client):
    image = 'docker.io/' + TEST_IMAGE
    uuid = 'docker:' + image
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')
    container = super_client.wait_success(container)
    assert container.state == 'running'
    assert container.data.dockerContainer.Image == image
    if container is not None:
        docker_client.delete(container)


@if_docker
def test_docker_command(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge',
                                               command=['sleep', '42'])

    try:
        container = super_client.wait_success(container)
        assert container.data.dockerContainer.Command == 'sleep 42'
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_docker_command_args(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge',
                                               command=['sleep', '1', '2',
                                                        '3'])

    try:
        container = super_client.wait_success(container)
        assert container.data.dockerContainer.Command == 'sleep 1 2 3'
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_short_lived_container(docker_client, super_client):
    container = docker_client.create_container(imageUuid="docker:tianon/true",
                                               networkMode='bridge')
    container = wait_for_condition(
        docker_client, container,
        lambda x: x.state == 'stopped',
        lambda x: 'State is: ' + x.state)

    assert container.state == 'stopped'
    assert container.transitioning == 'no'


@if_docker
def test_docker_stop(docker_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')

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
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')

    assert container.state == 'creating'

    container = docker_client.wait_success(container)

    assert container.state == 'running'

    container = container.stop(timeout=0)
    assert container.state == 'stopping'

    container = docker_client.wait_success(container)
    assert container.state == 'stopped'

    docker_client.delete(container)

    container = docker_client.wait_success(container)
    assert container.removed is not None

    container = docker_client.wait_success(container.purge())
    assert container.state == 'purged'

    volumes = container.volumes()
    assert len(volumes) == 0


@if_docker
def test_docker_image_format(docker_client, super_client):
    uuid = TEST_IMAGE_UUID
    container = docker_client.create_container(imageUuid=uuid,
                                               networkMode='bridge')

    try:
        container = docker_client.wait_success(container)
        container = super_client.reload(container)

        assert container.image().format == 'docker'
        assert container.volumes()[0].image().format == 'docker'
        assert container.volumes()[0].format == 'docker'
    finally:
        if container is not None:
            docker_client.delete(container)


@if_docker
def test_docker_ports_from_container_publish_all(docker_client):
    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(networkMode='bridge',
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

    docker_client.delete(c)


@if_docker
def test_docker_ports_from_container_no_publish(docker_client):
    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(imageUuid=uuid,
                                       networkMode='bridge')

    c = docker_client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports_link()
    assert len(ports) == 1
    port = ports[0]

    assert port.publicPort is None
    assert port.privatePort == 8080
    assert port.publicIpAddressId is not None
    assert port.kind == 'imagePort'

    docker_client.delete(c)


@if_docker
def test_docker_ports_from_container(docker_client, super_client):
    def reload(x):
        return super_client.reload(x)

    _ = reload

    uuid = TEST_IMAGE_UUID
    c = docker_client.create_container(networkMode='bridge',
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

    count = 0
    ip = None
    privateIp = None
    for port in c.ports_link():
        count += 1
        assert port.privateIpAddressId is not None
        privateIp = port.privateIpAddress()

        assert privateIp.kind == 'docker'
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
    if c.state != 'running':
        super_c = super_client.reload(c)
        print 'DEBUG Container not running: %s' % super_c
    assert c.state == 'running'

    count = 0
    for nic in _(c).nics():
        for ip in nic.ipAddresses():
            count += 1
            assert ip.kind == 'docker'
            assert ip.state == 'active'
            assert ip.address is not None

    assert count == 1

    docker_client.delete(c)


@if_docker
def test_docker_bind_address(docker_client, super_client):
    c = docker_client.create_container(name='bindAddrTest',
                                       networkMode='bridge',
                                       imageUuid=TEST_IMAGE_UUID,
                                       ports=['127.0.0.1:89:8999'])
    c = docker_client.wait_success(c)
    assert c.state == 'running'

    c = super_client.reload(c)
    bindings = c.data['dockerInspect']['HostConfig']['PortBindings']
    assert bindings['8999/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '89'}]

    c = docker_client.create_container(name='bindAddrTest2',
                                       networkMode='bridge',
                                       imageUuid=TEST_IMAGE_UUID,
                                       ports=['127.2.2.2:89:8999'])
    c = docker_client.wait_success(c)
    assert c.state == 'running'
    c = super_client.reload(c)
    bindings = c.data['dockerInspect']['HostConfig']['PortBindings']
    assert bindings['8999/tcp'] == [{'HostIp': '127.2.2.2', 'HostPort': '89'}]

    c = docker_client.create_container(name='bindAddrTest3',
                                       networkMode='bridge',
                                       imageUuid=TEST_IMAGE_UUID,
                                       ports=['127.2.2.2:89:8999'])
    c = docker_client.wait_transitioning(c)
    assert c.transitioning == 'error'
    assert 'Allocation failed: host needs ports 89/tcp available' in \
           c.transitioningMessage

    assert c.state == 'error'


@if_docker
def test_no_port_override(docker_client, super_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       networkMode='bridge',
                                       ports=['8083:8080'])

    try:
        c = super_client.wait_success(c, timeout=240)

        assert c.state == 'running'
        ports = c.ports_link()

        assert len(ports) == 1
        assert ports[0].kind == 'userPort'
        assert ports[0].publicPort == 8083
        assert ports[0].privatePort == 8080
    finally:
        if c is not None:
            super_client.delete(c)


@if_docker
def test_docker_volumes(docker_client, super_client):
    def reload(x):
        return super_client.reload(x)

    _ = reload

    uuid = TEST_IMAGE_UUID
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path

    c = docker_client.create_container(imageUuid=uuid,
                                       networkMode='bridge',
                                       startOnCreate=False,
                                       dataVolumes=['/foo',
                                                    bar_bind_mount])

    c = docker_client.wait_success(c)
    assert len(c.dataVolumes) == 2
    assert set(c.dataVolumes) == set(['/foo', bar_bind_mount])

    c = super_client.wait_success(c.start())

    volumes = c.volumes()
    assert len(volumes) == 1

    mounts = docker_client.reload(c).mounts()
    assert len(mounts) == 2
    foo_mount, bar_mount = None, None
    foo_vol, bar_vol = None, None
    for mount in mounts:
        assert mount.instance().id == c.id
        if mount.path == '/foo':
            foo_mount = mount
            foo_vol = mount.volume()
        elif mount.path == '/bar':
            bar_mount = mount
            bar_vol = mount.volume()

    foo_vol = wait_for_condition(
        docker_client, foo_vol, lambda x: x.state == 'active')
    assert foo_mount is not None
    assert foo_mount.permissions == 'rw'
    assert foo_vol is not None
    assert not foo_vol.isHostPath
    assert _(foo_vol).attachedState == 'inactive'

    bar_vol = wait_for_condition(
        docker_client, bar_vol, lambda x: x.state == 'active')
    assert bar_mount is not None
    assert bar_mount.permissions == 'rw'
    assert bar_vol is not None
    assert _(bar_vol).attachedState == 'inactive'
    assert bar_vol.isHostPath
    # We use 'in' instead of '==' because Docker uses the fully qualified
    # non-linked path and it might look something like: /mnt/sda1/<path>
    assert bar_host_path in bar_vol.uri

    c2 = docker_client.create_container(name="volumes_from_test",
                                        networkMode='bridge',
                                        imageUuid=uuid,
                                        startOnCreate=False,
                                        dataVolumesFrom=[c.id])
    c2 = docker_client.wait_success(c2)
    assert len(c2.dataVolumesFrom) == 1
    assert set(c2.dataVolumesFrom) == set([c.id])

    c2 = super_client.wait_success(c2.start())
    c2_mounts = docker_client.reload(c2).mounts()
    assert len(c2_mounts) == 2

    for mount in c2_mounts:
        assert mount.instance().id == c2.id
        if mount.path == '/foo':
            assert mount.volumeId == foo_vol.id
        elif mount.path == '/bar':
            assert mount.volumeId == bar_vol.id

    c = docker_client.wait_success(c.stop(remove=True, timeout=0))
    c2 = docker_client.wait_success(c2.stop(remove=True, timeout=0))

    _check_path(foo_vol, False, docker_client, super_client)
    _check_path(bar_vol, True, docker_client, super_client)


@if_docker
def test_volumes_from_more_than_one_container(docker_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       networkMode='bridge',
                                       dataVolumes=['/foo'])
    docker_client.wait_success(c)

    c2 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                        networkMode='bridge',
                                        dataVolumes=['/bar'])
    docker_client.wait_success(c2)

    c3 = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                        networkMode='bridge',
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
                                       networkMode='bridge',
                                       imageUuid=image_uuid,
                                       capAdd=caps,
                                       capDrop=caps,
                                       dnsSearch=['8.8.8.8', '1.2.3.4'],
                                       dns=['8.8.8.8', '1.2.3.4'],
                                       privileged=True,
                                       domainName="rancher.io",
                                       memory=12000000,
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

    wait_for(lambda: super_client.reload(c).data['dockerInspect'] is not None)
    wait_for(lambda: super_client.
             reload(c).data['dockerInspect']['HostConfig'] is not None)

    assert set(c.data['dockerInspect']['HostConfig']['CapAdd']) == set(caps)
    assert set(c.data['dockerInspect']['HostConfig']['CapDrop']) == set(caps)
    actual_dns = c.data['dockerInspect']['HostConfig']['Dns']
    # TODO: when networking is back
    # assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4', '169.254.169.250'])
    assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4'])
    actual_dns = c.data['dockerInspect']['HostConfig']['DnsSearch']
    # TODO: when networking is back
    # assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4', 'rancher.internal'])
    assert set(actual_dns) == set(['8.8.8.8', '1.2.3.4'])
    assert c.data['dockerInspect']['HostConfig']['Privileged']
    assert c.data['dockerInspect']['Config']['Domainname'] == "rancher.io"
    assert c.data['dockerInspect']['HostConfig']['Memory'] == 12000000
    # assert c.data['dockerInspect']['Config']['MemorySwap'] == 16000000
    assert c.data['dockerInspect']['HostConfig']['CpusetCpus'] == "0,1"
    assert c.data['dockerInspect']['Config']['Tty']
    assert c.data['dockerInspect']['Config']['OpenStdin']
    actual_entry_point = set(c.data['dockerInspect']['Config']['Entrypoint'])
    assert actual_entry_point == set(["/bin/sh", "-c"])
    assert c.data['dockerInspect']['HostConfig']['CpuShares'] == 400
    act_restart_pol = c.data['dockerInspect']['HostConfig']['RestartPolicy']
    assert act_restart_pol['MaximumRetryCount'] == 2
    assert act_restart_pol['Name'] == "on-failure"
    actual_devices = c.data['dockerInspect']['HostConfig']['Devices']
    assert len(actual_devices) == 1
    assert actual_devices[0]['CgroupPermissions'] == "rw"
    assert actual_devices[0]['PathOnHost'] == "/dev/null"
    assert actual_devices[0]['PathInContainer'] == "/dev/xnull"


def get_mounts(resource):
    return [x for x in resource.mounts() if x.state != 'inactive']


def check_mounts(client, resource, count):
    def wait_for_mount_count(res):
        m = get_mounts(res)
        return len(m) == count

    wait_for_condition(client, resource, wait_for_mount_count)
    mounts = get_mounts(resource)
    return mounts


def volume_cleanup_setup(docker_client, uuid, strategy=None):
    labels = {}
    if strategy:
        labels[VOLUME_CLEANUP_LABEL] = strategy

    vol_name = random_str()
    c = docker_client.create_container(name="volume_cleanup_test",
                                       imageUuid=uuid,
                                       networkMode='bridge',
                                       dataVolumes=['/tmp/foo',
                                                    '%s:/foo' % vol_name],
                                       labels=labels)
    c = docker_client.wait_success(c)
    if strategy:
        assert c.labels[VOLUME_CLEANUP_LABEL] == strategy

    mounts = check_mounts(docker_client, c, 2)
    v1 = mounts[0].volume()
    v2 = mounts[1].volume()
    wait_for_condition(docker_client, v1, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    wait_for_condition(docker_client, v2, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    named_vol = v1 if v1.name == vol_name else v2
    unnamed_vol = v1 if v1.name != vol_name else v2

    def purged(c):
        c = docker_client.reload(c)
        if c.state == 'purged':
            return True
        try:
            c.purge()
        except:
            pass
    c = docker_client.wait_success(c.stop(remove=True, timeout=0))
    wait_for(lambda: purged(c))
    check_mounts(docker_client, c, 0)
    return c, named_vol, unnamed_vol


@if_docker
def test_cleanup_volume_strategy(docker_client):
    c, named_vol, unnamed_vol = volume_cleanup_setup(docker_client,
                                                     TEST_IMAGE_UUID)
    assert docker_client.wait_success(named_vol).state == 'detached'
    assert docker_client.wait_success(unnamed_vol).removed is not None

    c, named_vol, unnamed_vol = volume_cleanup_setup(docker_client,
                                                     TEST_IMAGE_UUID,
                                                     strategy='unnamed')
    assert docker_client.wait_success(named_vol).state == 'detached'
    assert docker_client.wait_success(unnamed_vol).removed is not None

    c, named_vol, unnamed_vol = volume_cleanup_setup(docker_client,
                                                     TEST_IMAGE_UUID,
                                                     strategy='none')
    assert docker_client.wait_success(named_vol).state == 'detached'
    assert docker_client.wait_success(unnamed_vol).state == 'detached'

    c, named_vol, unnamed_vol = volume_cleanup_setup(docker_client,
                                                     TEST_IMAGE_UUID,
                                                     strategy='all')
    assert docker_client.wait_success(named_vol).removed is not None
    assert docker_client.wait_success(unnamed_vol).removed is not None


@if_docker
def test_docker_mount_life_cycle(docker_client):
    # Using nginx because it has a baked in volume, which is a good test case
    uuid = 'docker:nginx:1.9.0'
    bind_mount_uuid = py_uuid.uuid4().hex
    bar_host_path = '/tmp/bar%s' % bind_mount_uuid
    bar_bind_mount = '%s:/bar' % bar_host_path

    c = docker_client.create_container(imageUuid=uuid,
                                       startOnCreate=False,
                                       networkMode='bridge',
                                       dataVolumes=['%s:/foo' % random_str(),
                                                    bar_bind_mount])

    c = docker_client.wait_success(c)
    c = docker_client.wait_success(c.start())
    mounts = check_mounts(docker_client, c, 3)
    v1 = mounts[0].volume()
    v2 = mounts[1].volume()
    v3 = mounts[2].volume()
    wait_for_condition(docker_client, v1, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    wait_for_condition(docker_client, v2, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    wait_for_condition(docker_client, v3, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)

    c = docker_client.wait_success(c.stop(timeout=0))
    assert c.state == 'stopped'
    wait_for_condition(docker_client, v1, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    wait_for_condition(docker_client, v2, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)
    wait_for_condition(docker_client, v3, lambda x: x.state == 'active',
                       lambda x: 'state is %s' % x)

    c = docker_client.wait_success(c.remove())
    check_mounts(docker_client, c, 0)
    # State can be either detached or removed depending on whether c got purged
    assert docker_client.wait_success(v1).state != 'active'
    assert docker_client.wait_success(v2).state != 'active'
    assert docker_client.wait_success(v3).state != 'active'


@if_docker
def test_docker_labels(docker_client, super_client):
    # 1.8 broke this behavior where labels would come from the images
    # one day maybe they will bring it back.
    # image_uuid = 'docker:ranchertest/labelled:v0.1.0'
    image_uuid = TEST_IMAGE_UUID

    c = docker_client.create_container(name="labels_test",
                                       imageUuid=image_uuid,
                                       networkMode='bridge',
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

    sc = super_client.reload(c)
    mac_address = sc.nics()[0].macAddress

    expected_labels = {
        # 'io.rancher.testlabel': 'value1',
        # 'io.rancher.testlabel.space': 'value 1',
        'io.rancher.testlabel.fromapi': 'yes',
        'io.rancher.container.uuid': c.uuid,
        'io.rancher.container.name': c.name,
        'io.rancher.container.mac_address': mac_address,
    }
    assert actual_labels == expected_labels

    docker_client.delete(c)


@if_docker
def test_container_odd_fields(super_client, docker_client):
    c = docker_client.create_container(pidMode=None,
                                       imageUuid=TEST_IMAGE_UUID,
                                       networkMode='bridge',
                                       logConfig={
                                           'driver': None,
                                           'config': None,
                                       })
    c = docker_client.wait_success(c)

    assert c.state == 'running'
    assert c.pidMode is None
    assert c.logConfig == {'type': 'logConfig', 'driver': None, 'config': None}

    c = super_client.reload(c)

    assert c.data.dockerInspect.HostConfig.LogConfig['Type'] == 'json-file'
    assert not c.data.dockerInspect.HostConfig.LogConfig['Config']


@if_docker
def test_container_bad_build(super_client, docker_client):
    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       networkMode='bridge',
                                       build={
                                           'context': None,
                                           'remote': None
                                       })
    c = docker_client.wait_success(c)

    assert c.state == 'running'
    assert c.pidMode is None
    assert c.build == {'context': None, 'remote': None, 'type': 'dockerBuild'}

    c = super_client.reload(c)

    assert c.data.dockerInspect.Config.Image == TEST_IMAGE


@if_docker
def test_service_link_emu_docker_link(super_client, docker_client):
    env_name = random_str()
    env = docker_client.create_environment(name=env_name)
    env = docker_client.wait_success(env)
    assert env.state == "active"

    server = docker_client.create_service(name='server', launchConfig={
        'networkMode': 'bridge',
        'imageUuid': TEST_IMAGE_UUID
    }, environmentId=env.id)

    service = docker_client.create_service(name='client', launchConfig={
        'networkMode': 'bridge',
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

    assert link.targetInstanceId == target_instance.id
    assert link.instanceNames == ['{}-server-1'.format(env_name)]

    docker_client.delete(env)


@if_docker
def test_service_links_with_no_ports(docker_client):
    env = docker_client.create_environment(name=random_str())
    env = docker_client.wait_success(env)
    assert env.state == "active"

    server = docker_client.create_service(name='server', launchConfig={
        'imageUuid': TEST_IMAGE_UUID,
        'networkMode': 'bridge',
        'stdinOpen': True,
        'tty': True,
    }, environmentId=env.id)
    server = docker_client.wait_success(server)
    assert server.state == 'inactive'

    service = docker_client.create_service(name='client', launchConfig={
        'imageUuid': TEST_IMAGE_UUID,
        'networkMode': 'bridge',
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


@pytest.mark.skipif('True')
def test_blkio_device_options(super_client, docker_client):
    dev_opts = {
        '/dev/sda': {
            'readIops': 1000,
            'writeIops': 2000,
        },
        '/dev/null': {
            'readBps': 3000,
        }
    }

    c = docker_client.create_container(imageUuid=TEST_IMAGE_UUID,
                                       networkMode=None,
                                       blkioDeviceOptions=dev_opts)
    c = docker_client.wait_success(c)
    assert c.state == 'running'

    super_c = super_client.reload(c)
    hc = super_c.data.dockerInspect['HostConfig']
    assert hc['BlkioDeviceReadIOps'] == [{'Path': '/dev/sda', 'Rate': 1000}]
    assert hc['BlkioDeviceWriteIOps'] == [{'Path': '/dev/sda', 'Rate': 2000}]
    assert hc['BlkioDeviceReadBps'] == [{'Path': '/dev/null', 'Rate': 3000}]


def _check_path(volume, should_exist, client, super_client):
    path = _path_to_volume(volume)
    print 'Checking path [%s] for volume [%s].' % (path, volume)
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
