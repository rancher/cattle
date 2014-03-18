from dstack import ApiError
from common_fixtures import *  # NOQA
from datetime import timedelta
import time


def test_container_create_only(admin_client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = admin_client.create_container(name="test",
                                        imageUuid=uuid,
                                        startOnCreate=False)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "creating",
        "imageUuid": uuid,
        "imageId": NOT_NONE,
    })

    container = wait_success(admin_client, container)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "stopped",
        "imageUuid": uuid,
        "imageId": NOT_NONE,
    })

    image = container.image()
    image = wait_success(admin_client, image)
    assert_fields(image, {
        "state": "inactive"
    })

    volumes = container.volumes()
    assert len(volumes) == 1

    root_volume = wait_success(admin_client, volumes[0])
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

    image = wait_success(admin_client, admin_client.list_image(uuid=uuid)[0])
    assert_fields(image, {
        "state": "inactive",
        "uuid": uuid,
        "isPublic": True,
    })
    image_mappings = image.imageStoragePoolMaps()

    assert len(image_mappings) == 1

    image_mapping = wait_success(admin_client, image_mappings[0])
    assert_fields(image_mapping, {
        "imageId": image.id,
        "storagePoolId": sim_context["external_pool"].id,
        "state": "inactive",
    })

    return container


def test_container_create_then_start(admin_client, sim_context):
    container = test_container_create_only(admin_client, sim_context)
    container = container.start()

    assert_fields(container, {
        "state": "starting"
    })

    container = wait_success(admin_client, container)

    assert_fields(container, {
        "allocationState": "active",
        "state": "running"
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


def test_container_stop(admin_client, sim_context):
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
        "allocationState": "inactive",
        "state": "stopped"
    })

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
    assert instance_host_mappings[0].state == "removed"
    assert instance_host_mappings[0].removed is not None


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


def test_container_remove(admin_client, sim_context):
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

    return _assert_removed(container)


def test_container_restore(admin_client, sim_context):
    container = test_container_remove(admin_client, sim_context)

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

    volume_mappings = volumes[0].volumeStoragePoolMaps()
    assert len(volume_mappings) == 1
    assert volume_mappings[0].state == "inactive"


def test_container_purge(admin_client, sim_context):
    container = test_container_remove(admin_client, sim_context)

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

    volume = container.volumes()[0]
    assert volume.state == "removed"

    volume = volume.purge()
    assert volume.state == 'purging'

    volume = wait_transitioning(admin_client, volume)
    assert volume.state == 'purged'

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


def test_container_compute_fail(admin_client, sim_context):
    data = {
        'compute.instance.activate::fail': True,
        'io.github.ibuildthecloud.dstack.process.instance.InstanceStart': {
            'computeTries': 1
        }
    }

    container = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                        data=data)

    container = wait_transitioning(admin_client, container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [compute.instance.activate]'

    _assert_removed(container)


def test_container_storage_fail(admin_client, sim_context):
    data = {
        'storage.volume.activate::fail': True,
    }

    container = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                        data=data)

    container = wait_transitioning(admin_client, container)

    assert container.transitioning == 'error'
    assert container.transitioningMessage == \
        'Failing [storage.volume.activate]'

    _assert_removed(container)
