import base64
import json

from cattle import ApiError
from common_fixtures import *  # NOQA
from datetime import timedelta
import time


def test_container_create_count(admin_client, sim_context):
    image_uuid = sim_context['imageUuid']

    cs = admin_client.create_container(imageUuid=image_uuid, count=3)

    assert len(cs) == 3

    for c in cs:
        c = admin_client.wait_success(c)
        assert c.state == 'running'


def test_container_create_only(admin_client, internal_test_client,
                               sim_context):
    uuid = "sim:{}".format(random_num())
    container = admin_client.create_container(name="test",
                                              imageUuid=uuid,
                                              startOnCreate=False)

    assert_fields(container, {
        "type": "container",
        "instanceTriggeredStop": "stop",
        "allocationState": "inactive",
        "state": "creating",
        "imageUuid": uuid,
        "firstRunning": None,
    })

    container = wait_success(admin_client, container)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "stopped",
        "imageUuid": uuid,
    })

    container = internal_test_client.reload(container)

    assert container.imageId is not None

    image = container.image()
    image = wait_success(internal_test_client, image)
    assert_fields(image, {
        "state": "active"
    })

    volumes = container.volumes()
    assert len(volumes) == 1

    root_volume = wait_success(internal_test_client, volumes[0])
    assert_fields(root_volume, {
        "allocationState": "inactive",
        "attachedState": "active",
        "state": "inactive",
        "instanceId": container.id,
        "deviceNumber": 0,
    })

    volume_mappings = root_volume.volumeStoragePoolMaps()
    assert len(volume_mappings) == 0

    # No nics right now
    #
    # nics = container.nics()
    # assert len(nics) == 0

    image = wait_success(internal_test_client,
                         internal_test_client.list_image(uuid=uuid)[0])
    assert_fields(image, {
        "state": "active",
        "uuid": uuid,
        "isPublic": True,
    })
    image_mappings = image.imageStoragePoolMaps()

    assert len(image_mappings) == 1

    image_mapping = wait_success(internal_test_client, image_mappings[0])
    assert_fields(image_mapping, {
        "imageId": image.id,
        "storagePoolId": sim_context["external_pool"].id,
        "state": "inactive",
    })

    return admin_client.reload(container)


def _assert_running(container, sim_context):
    assert_fields(container, {
        "allocationState": "active",
        "state": "running",
        "firstRunning": NOT_NONE
    })

    root_volume = container.volumes()[0]
    assert_fields(root_volume, {
        "state": "active"
    })

    image = root_volume.image()
    assert_fields(image, {
        "state": "active"
    })

    volume_mappings = root_volume.volumeStoragePoolMaps()
    assert len(volume_mappings) == 1

    assert_fields(volume_mappings[0], {
        "state": "active"
    })

    volume_pool = volume_mappings[0].storagePool()
    assert_fields(volume_pool, {
        "state": "active"
    })

    image_mappings = image.imageStoragePoolMaps()
    assert len(image_mappings) == 2
    for image_mapping in image_mappings:
        if image_mapping.storagePoolId == sim_context["external_pool"].id:
            pass
        else:
            assert_fields(image_mapping, {
                "state": "active",
                "storagePoolId": volume_pool.id
            })

    instance_host_mappings = container.instanceHostMaps()
    assert len(instance_host_mappings) == 1

    assert_fields(instance_host_mappings[0], {
        "state": "active"
    })


def test_container_create_then_start(admin_client, internal_test_client,
                                     sim_context):
    container = test_container_create_only(admin_client, internal_test_client,
                                           sim_context)
    container = container.start()

    assert_fields(container, {
        "state": "starting"
    })
    container = internal_test_client.reload(container)
    container = wait_success(internal_test_client, container)

    _assert_running(container, sim_context)


def test_container_first_running(admin_client, sim_context):
    c = admin_client.create_container(
        imageUuid=sim_context['imageUuid'],
        startOnCreate=False)
    c = admin_client.wait_success(c)

    assert c.state == 'stopped'
    assert c.firstRunning is None

    c = admin_client.wait_success(c.start())
    assert c.state == 'running'
    assert c.firstRunning is not None

    first = c.firstRunning

    c = admin_client.wait_success(c.restart())
    assert c.state == 'running'
    assert c.firstRunning == first


def test_container_restart(admin_client, internal_test_client, sim_context):
    container = test_container_create_only(admin_client, internal_test_client,
                                           sim_context)
    container = container.start()
    container = internal_test_client.reload(container)
    container = wait_success(internal_test_client, container)
    _assert_running(container, sim_context)
    container = admin_client.reload(container)
    container = container.restart()

    assert container.state == 'restarting'
    container = wait_success(admin_client, container)
    container = internal_test_client.reload(container)
    _assert_running(container, sim_context)


def test_container_stop(admin_client, internal_test_client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = admin_client.create_container(name="test",
                                              imageUuid=uuid,
                                              startOnCreate=True)
    container = wait_success(admin_client, container)

    assert_fields(container, {
        "state": "running"
    })

    container = container.stop()

    assert_fields(container, {
        "state": "stopping"
    })

    container = wait_success(admin_client, container)

    assert_fields(container, {
        "allocationState": "active",
        "state": "stopped"
    })

    container = internal_test_client.reload(container)
    root_volume = container.volumes()[0]
    assert_fields(root_volume, {
        "state": "inactive"
    })

    image = root_volume.image()
    assert_fields(image, {
        "state": "active"
    })

    volume_mappings = root_volume.volumeStoragePoolMaps()
    assert len(volume_mappings) == 1

    assert_fields(volume_mappings[0], {
        "state": "inactive"
    })

    volume_pool = volume_mappings[0].storagePool()
    assert_fields(volume_pool, {
        "state": "active"
    })

    image_mappings = image.imageStoragePoolMaps()
    assert len(image_mappings) == 2
    for image_mapping in image_mappings:
        if image_mapping.storagePoolId == sim_context["external_pool"].id:
            pass
        else:
            assert_fields(image_mapping, {
                "state": "active",
                "storagePoolId": volume_pool.id
            })

    instance_host_mappings = container.instanceHostMaps()
    assert len(instance_host_mappings) == 1
    assert instance_host_mappings[0].state == 'inactive'


def _assert_removed(container):
    assert container.state == "removed"
    assert_removed_fields(container)

    volumes = container.volumes()
    assert len(volumes) == 1

    assert volumes[0].state == "removed"
    assert_removed_fields(volumes[0])

    volume_mappings = volumes[0].volumeStoragePoolMaps()
    assert len(volume_mappings) == 1
    assert volume_mappings[0].state == "inactive"

    return container


def test_container_remove(admin_client, internal_test_client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = admin_client.create_container(name="test",
                                              imageUuid=uuid,
                                              startOnCreate=True)
    container = wait_success(admin_client, container)
    container = wait_success(admin_client, container.stop())

    assert container.state == "stopped"

    container = admin_client.delete(container)

    assert container.state == "removing"

    container = wait_success(admin_client, container)
    container = internal_test_client.reload(container)
    container = _assert_removed(container)
    return admin_client.reload(container)


def test_container_delete_while_running(admin_client, internal_test_client,
                                        sim_context):
    uuid = "sim:{}".format(random_num())
    container = admin_client.create_container(name="test",
                                              imageUuid=uuid)
    container = admin_client.wait_success(container)
    assert container.state == 'running'

    container = admin_client.delete(container)
    assert container.state == 'stopping'

    container = wait_success(admin_client, container)
    container = internal_test_client.reload(container)
    return _assert_removed(container)


def test_container_restore(admin_client, internal_test_client, sim_context):
    container = test_container_remove(admin_client, internal_test_client,
                                      sim_context)

    assert container.state == "removed"

    container = container.restore()

    assert container.state == "restoring"

    container = wait_success(admin_client, container)

    assert container.state == "stopped"
    assert_restored_fields(container)

    volumes = container.volumes()
    assert len(volumes) == 1

    assert volumes[0].state == "inactive"
    assert_restored_fields(volumes[0])
    container = internal_test_client.reload(container)
    volumes = container.volumes()
    volume_mappings = volumes[0].volumeStoragePoolMaps()
    assert len(volume_mappings) == 1
    assert volume_mappings[0].state == "inactive"


def test_container_purge(admin_client, internal_test_client, sim_context):
    container = test_container_remove(admin_client, internal_test_client,
                                      sim_context)

    assert container.state == "removed"

    # It's easier to call container.purge(), but this was to test other
    # things too

    remove_time = now() - timedelta(hours=1)
    admin_client.update(container, {
        'removeTime': format_time(remove_time)
    })

    purge = admin_client.list_task(name="purge.resources")[0]
    purge.execute()

    container = admin_client.reload(container)
    for x in range(30):
        if container.state == "removed":
            time.sleep(0.5)
            container = admin_client.reload(container)
        else:
            break

    assert container.state != "removed"

    container = wait_success(admin_client, container)
    assert container.state == "purged"

    container = internal_test_client.reload(container)
    instance_host_mappings = container.instanceHostMaps()
    assert len(instance_host_mappings) == 1
    assert instance_host_mappings[0].state == "removed"
    assert instance_host_mappings[0].removed is not None

    volume = container.volumes()[0]
    assert volume.state == "removed"

    volume = volume.purge()
    assert volume.state == 'purging'

    volume = wait_transitioning(admin_client, volume)
    assert volume.state == 'purged'
    container = internal_test_client.reload(container)
    volume = container.volumes()[0]
    pool_maps = volume.volumeStoragePoolMaps()
    assert len(pool_maps) == 1
    assert pool_maps[0].state == 'removed'


def test_start_stop(admin_client, sim_context):
    uuid = "sim:{}".format(random_num())

    container = admin_client.create_container(name="test",
                                              imageUuid=uuid)

    container = wait_success(admin_client, container)

    for _ in range(5):
        assert container.state == 'running'
        container = wait_success(admin_client, container.stop())
        assert container.state == 'stopped'
        container = wait_success(admin_client, container.start())
        assert container.state == 'running'


def test_container_image_required(client, sim_context):
    try:
        client.create_container()
        assert False
    except ApiError as e:
        assert e.error.status == 422
        assert e.error.code == 'MissingRequired'
        assert e.error.fieldName == 'imageUuid'


def test_container_compute_fail(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']

    data = {
        'compute.instance.activate::fail': True,
        'io.cattle.platform.process.instance.InstanceStart': {
            'computeTries': 1
        }
    }

    container = internal_test_client.create_container(imageUuid=image_uuid,
                                                      data=data)

    container = wait_transitioning(internal_test_client, container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [compute.instance.activate]'

    _assert_removed(container)


def test_container_storage_fail(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']

    data = {
        'storage.volume.activate::fail': True,
    }

    container = internal_test_client.create_container(imageUuid=image_uuid,
                                                      data=data)

    container = wait_transitioning(internal_test_client, container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [storage.volume.activate]'

    _assert_removed(container)


def test_create_with_vnet(internal_test_client, sim_context):
    network = create_and_activate(internal_test_client, 'network')

    subnet1 = create_and_activate(internal_test_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.0.0')
    create_and_activate(internal_test_client, 'subnet',
                        networkId=network.id,
                        networkAddress='192.168.1.0')

    vnet = create_and_activate(internal_test_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    create_and_activate(internal_test_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet1.id)

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        vnetIds=[vnet.id])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'
    assert 'vnetIds' not in c

    nics = c.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.deviceNumber == 0
    assert nic.vnetId == vnet.id
    assert nic.subnetId == subnet1.id


def test_create_with_vnet2(internal_test_client, sim_context):
    network = create_and_activate(internal_test_client, 'network')

    create_and_activate(internal_test_client, 'subnet',
                        networkId=network.id,
                        networkAddress='192.168.0.0')
    subnet2 = create_and_activate(internal_test_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.1.0')

    vnet = create_and_activate(internal_test_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    create_and_activate(internal_test_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet2.id)

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        vnetIds=[vnet.id])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'
    assert 'vnetIds' not in c

    nics = c.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.deviceNumber == 0
    assert nic.vnetId == vnet.id
    assert nic.subnetId == subnet2.id


def test_create_with_vnet_multiple_nics(internal_test_client, sim_context):
    network = create_and_activate(internal_test_client, 'network')

    subnet1 = create_and_activate(internal_test_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.0.0')
    subnet2 = create_and_activate(internal_test_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.1.0')

    vnet = create_and_activate(internal_test_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    vnet2 = create_and_activate(internal_test_client, 'vnet',
                                networkId=network.id,
                                uri='dummy:///')

    create_and_activate(internal_test_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet2.id)

    create_and_activate(internal_test_client, 'subnetVnetMap',
                        vnetId=vnet2.id,
                        subnetId=subnet1.id)

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        vnetIds=[vnet.id, vnet2.id])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'
    assert 'vnetIds' not in c

    nics = c.nics()
    assert len(nics) == 2

    device_numbers = set([n.deviceNumber for n in nics])
    assert len(device_numbers) == 2
    assert 0 in device_numbers
    assert 1 in device_numbers

    for nic in nics:
        assert nic.state == 'active'

        if nic.deviceNumber == 0:
            nic.subnetId == subnet2.id
            nic.vnetId == vnet.id
        elif nic.deviceNumber == 1:
            nic.subnetId == subnet1.id
            nic.vnetId == vnet2.id


def test_container_exec_on_stop(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=sim_context['host'].id)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    assert callable(c.execute)

    c = admin_client.wait_success(c.stop())

    assert 'execute' not in c


def test_container_exec(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=sim_context['host'].id)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    assert callable(c.execute)

    resp = c.execute(command=['/bin/sh'])

    assert resp.url is not None
    assert resp.token is not None

    jwt = _get_jwt(resp.token)

    assert jwt['exec']['AttachStdin']
    assert jwt['exec']['AttachStdout']
    assert jwt['exec']['Tty']
    assert jwt['exec']['Cmd'] == ['/bin/sh']
    assert jwt['exec']['Container'] == c.uuid
    assert jwt['exp'] is not None

    resp = c.execute(command=['/bin/sh2', 'blah'], attachStdin=False,
                     attachStdout=False, tty=False)

    assert resp.url is not None
    assert resp.token is not None

    jwt = _get_jwt(resp.token)

    assert not jwt['exec']['AttachStdin']
    assert not jwt['exec']['AttachStdout']
    assert not jwt['exec']['Tty']
    assert jwt['exec']['Cmd'] == ['/bin/sh2', 'blah']

    admin_client.delete(c)


def _get_jwt(token):
    text = token.split('.')[1]
    missing_padding = 4 - len(text) % 4
    if missing_padding:
        text += '=' * missing_padding

    return json.loads(base64.b64decode(text))
