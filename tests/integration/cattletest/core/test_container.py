import base64
import json

from cattle import ApiError
from common_fixtures import *  # NOQA
from datetime import timedelta
import time


def test_container_create_count(client, sim_context):
    cs = create_container(client, sim_context,
                          count=3)

    assert len(cs) == 3

    for c in cs:
        c = client.wait_success(c)
        assert c.state == 'running'


def test_container_create_only(admin_client, super_client,
                               sim_context):
    uuid = "sim:{}".format(random_num())
    container = create_container(admin_client, sim_context,
                                 imageUuid=uuid,
                                 name="test",
                                 startOnCreate=False)

    container = super_client.reload(container)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "creating",
        "imageUuid": uuid,
        "firstRunning": None,
    })

    container = wait_success(super_client, container)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "stopped",
        "imageUuid": uuid,
    })

    container = super_client.reload(container)

    assert container.imageId is not None
    assert container.instanceTriggeredStop == 'stop'

    image = container.image()
    image = wait_success(super_client, image)
    assert_fields(image, {
        "state": "active"
    })

    volumes = container.volumes()
    assert len(volumes) == 1

    root_volume = wait_success(super_client, volumes[0])
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

    image = wait_success(super_client,
                         super_client.list_image(name=uuid)[0])
    assert_fields(image, {
        "state": "active",
        "name": uuid,
        "isPublic": False,
    })
    image_mappings = image.imageStoragePoolMaps()

    assert len(image_mappings) == 1

    image_mapping = wait_success(super_client, image_mappings[0])
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


def test_container_create_then_start(admin_client, super_client, sim_context):
    container = create_container(admin_client, sim_context)
    container = container.start()

    assert container.state == "starting"
    assert 'start' not in container
    assert 'stop' in container
    assert 'remove' in container

    _assert_running(super_client.wait_success(container), sim_context)


def test_container_first_running(admin_client, sim_context):
    c = create_container(admin_client, sim_context, startOnCreate=False)
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


def test_container_restart(admin_client, super_client, sim_context):
    container = create_container(admin_client, sim_context)
    container = admin_client.wait_success(container)

    _assert_running(super_client.reload(container), sim_context)

    container = admin_client.wait_success(container)
    container = container.restart()

    assert container.state == 'restarting'
    container = admin_client.wait_success(container)
    _assert_running(super_client.reload(container), sim_context)


def test_container_stop(admin_client, super_client, sim_context):
    container = create_container(admin_client, sim_context,
                                 name="test",
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

    assert_fields(super_client.reload(container), {
        "allocationState": "active",
        "state": "stopped"
    })

    container = super_client.reload(container)
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


def test_container_remove(admin_client, super_client, sim_context):
    container = create_container(admin_client, sim_context,
                                 name="test",
                                 startOnCreate=True)
    container = wait_success(admin_client, container)
    container = wait_success(admin_client, container.stop())

    assert container.state == "stopped"

    container = admin_client.delete(container)

    assert container.state == "removing"

    container = wait_success(admin_client, container)

    _assert_removed(super_client.reload(container))
    return container


def test_container_delete_while_running(admin_client, super_client,
                                        sim_context):
    container = create_container(admin_client, sim_context,
                                 name="test")
    container = admin_client.wait_success(container)
    assert container.state == 'running'

    container = admin_client.delete(container)
    assert container.state == 'stopping'

    container = wait_success(admin_client, container)
    _assert_removed(super_client.reload(container))
    return container


def test_container_restore(admin_client, super_client, sim_context):
    container = test_container_remove(admin_client, super_client,
                                      sim_context)

    assert container.state == "removed"

    container = container.restore()

    assert container.state == "restoring"

    container = wait_success(admin_client, container)

    assert container.state == "stopped"
    assert_restored_fields(super_client.reload(container))

    volumes = container.volumes()
    assert len(volumes) == 1

    assert volumes[0].state == "inactive"
    assert_restored_fields(super_client.reload(volumes[0]))

    volume_mappings = super_client.reload(volumes[0]).volumeStoragePoolMaps()
    assert len(volume_mappings) == 1
    assert volume_mappings[0].state == "inactive"


def test_container_purge(admin_client, super_client, sim_context):
    container = test_container_remove(admin_client, super_client,
                                      sim_context)

    assert container.state == "removed"

    # It's easier to call container.purge(), but this was to test other
    # things too

    remove_time = now() - timedelta(hours=1)
    super_client.update(container, {
        'removeTime': format_time(remove_time)
    })

    purge = super_client.list_task(name="purge.resources")[0]
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

    instance_host_mappings = super_client.reload(container).instanceHostMaps()
    assert len(instance_host_mappings) == 1
    assert instance_host_mappings[0].state == "removed"
    assert instance_host_mappings[0].removed is not None

    volume = container.volumes()[0]
    assert volume.state == "removed"

    volume = volume.purge()
    assert volume.state == 'purging'

    volume = wait_transitioning(admin_client, volume)
    assert volume.state == 'purged'

    pool_maps = super_client.reload(volume).volumeStoragePoolMaps()
    assert len(pool_maps) == 1
    assert pool_maps[0].state == 'removed'


def test_start_stop(admin_client, sim_context):
    container = create_container(admin_client, sim_context,
                                 name="test")

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


def test_container_compute_fail(super_client, sim_context):
    data = {
        'compute.instance.activate::fail': True,
        'io.cattle.platform.process.instance.InstanceStart': {
            'computeTries': 1
        }
    }

    container = create_container(super_client, sim_context,
                                 data=data)

    container = super_client.wait_transitioning(container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [compute.instance.activate]'

    _assert_removed(container)


def test_container_storage_fail(super_client, sim_context):
    data = {
        'storage.volume.activate::fail': True,
    }

    container = create_container(super_client, sim_context,
                                 data=data)
    container = super_client.wait_transitioning(container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [storage.volume.activate]'

    _assert_removed(container)


def test_create_with_vnet(super_client, sim_context):
    network = create_and_activate(super_client, 'network')

    subnet1 = create_and_activate(super_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.0.0')
    create_and_activate(super_client, 'subnet',
                        networkId=network.id,
                        networkAddress='192.168.1.0')

    vnet = create_and_activate(super_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    create_and_activate(super_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet1.id)

    c = create_container(super_client, sim_context,
                         vnetIds=[vnet.id])
    c = super_client.wait_success(c)
    assert c.state == 'running'
    assert 'vnetIds' not in c

    nics = c.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.deviceNumber == 0
    assert nic.vnetId == vnet.id
    assert nic.subnetId == subnet1.id


def test_create_with_vnet2(super_client, sim_context):
    network = create_and_activate(super_client, 'network')

    create_and_activate(super_client, 'subnet',
                        networkId=network.id,
                        networkAddress='192.168.0.0')
    subnet2 = create_and_activate(super_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.1.0')

    vnet = create_and_activate(super_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    create_and_activate(super_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet2.id)

    c = create_container(super_client, sim_context,
                         vnetIds=[vnet.id])
    c = super_client.wait_success(c)
    assert c.state == 'running'
    assert 'vnetIds' not in c

    nics = c.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.deviceNumber == 0
    assert nic.vnetId == vnet.id
    assert nic.subnetId == subnet2.id


def test_create_with_vnet_multiple_nics(super_client, sim_context):
    network = create_and_activate(super_client, 'network')

    subnet1 = create_and_activate(super_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.0.0')
    subnet2 = create_and_activate(super_client, 'subnet',
                                  networkId=network.id,
                                  networkAddress='192.168.1.0')

    vnet = create_and_activate(super_client, 'vnet',
                               networkId=network.id,
                               uri='dummy:///')

    vnet2 = create_and_activate(super_client, 'vnet',
                                networkId=network.id,
                                uri='dummy:///')

    create_and_activate(super_client, 'subnetVnetMap',
                        vnetId=vnet.id,
                        subnetId=subnet2.id)

    create_and_activate(super_client, 'subnetVnetMap',
                        vnetId=vnet2.id,
                        subnetId=subnet1.id)

    c = create_container(super_client, sim_context,
                         vnetIds=[vnet.id, vnet2.id])
    c = super_client.wait_success(c)
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


def test_container_restart_policy(admin_client, client):
    for c in [admin_client, client]:
        restart_policy = c.schema.types['restartPolicy']
        assert len(restart_policy.resourceFields) == 2
        assert 'name' in restart_policy.resourceFields
        assert 'maximumRetryCount' in restart_policy.resourceFields
        container = c.schema.types['container']
        assert 'restartPolicy' == \
               container.resourceFields['restartPolicy'].type


def test_container_exec_on_stop(admin_client, sim_context):
    c = create_container(admin_client, sim_context)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    assert callable(c.execute)

    c = admin_client.wait_success(c.stop())

    assert 'execute' not in c


def test_container_exec(admin_client, sim_context):
    c = create_container(admin_client, sim_context)
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
    assert jwt['exec']['Container'] == c.externalId
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


def test_container_logs(admin_client, sim_context):
    c = create_container(admin_client, sim_context)
    c = admin_client.wait_success(c)

    assert callable(c.logs)

    resp = c.logs(follow=True, lines=300)

    assert resp.url is not None
    assert resp.token is not None

    jwt = _get_jwt(resp.token)

    assert jwt['logs']['Container'] == c.externalId
    assert jwt['logs']['Lines'] == 300
    assert jwt['logs']['Follow'] is True
    assert jwt['exp'] is not None

    resp = c.logs()

    assert resp.url is not None
    assert resp.token is not None

    jwt = _get_jwt(resp.token)

    assert jwt['logs']['Container'] == c.externalId
    assert jwt['logs']['Lines'] == 100
    assert jwt['logs']['Follow'] is True
    assert jwt['exp'] is not None

    admin_client.delete(c)


def test_container_labels(client, sim_context):
    labels = {'affinity': "container==B", '!affinity': "container==C"}
    image_uuid = sim_context['imageUuid']
    container = client.create_container(name="test",
                                        imageUuid=image_uuid,
                                        labels=labels)
    container = client.wait_success(container)
    assert container.state == 'running'
    assert container.labels == labels


def _get_jwt(token):
    text = token.split('.')[1]
    missing_padding = 4 - len(text) % 4
    if missing_padding:
        text += '=' * missing_padding

    return json.loads(base64.b64decode(text))


def test_container_request_ip(super_client, sim_context, test_network):
    for i in range(2):
        # Doing this twice essentially ensure that the IP gets freed the first
        # time
        container = create_container(super_client, sim_context,
                                     networkIds=[test_network.id],
                                     startOnCreate=False)
        container = super_client.wait_success(container)
        assert container.state == 'stopped'
        container.data.fields['requestedIpAddress'] = '1.1.1.1'

        container = super_client.update(container, data=container.data)
        container = super_client.wait_success(container.start())

        assert container.primaryIpAddress == '1.1.1.1'

        # Try second time and should fail because it is used
        container2 = create_container(super_client, sim_context,
                                      networkIds=[test_network.id],
                                      startOnCreate=False)
        container2 = super_client.wait_success(container2)
        assert container2.state == 'stopped'
        container2.data.fields['requestedIpAddress'] = '1.1.1.1'

        container2 = super_client.update(container2, data=container2.data)
        container2 = super_client.wait_success(container2.start())

        assert container2.primaryIpAddress != '1.1.1.1'

        # Release 1.1.1.1
        container = super_client.wait_success(super_client.delete(container))
        container = super_client.wait_success(container.purge())

        ip = container.nics()[0].ipAddresses()[0]
        super_client.wait_success(ip.deactivate())


def test_container_network_modes(context, super_client):
    client = context['client']

    c = client.create_container(networkMode=None,
                                imageUuid=context['imageUuid'])
    c = super_client.wait_success(c)
    assert c.state == 'running'
    assert len(c.nics()) == 0

    for i in [('host', 'dockerHost'), ('none', 'dockerNone'),
              ('container', 'dockerContainer'), ('bridge', 'dockerBridge'),
              ('managed', 'hostOnlyNetwork')]:
        c = client.create_container(networkMode=i[0],
                                    imageUuid=context['imageUuid'])
        c = super_client.wait_success(c)
        assert c.state == 'running'
        assert len(c.nics()) == 1
        assert c.nics()[0].network().kind == i[1]
