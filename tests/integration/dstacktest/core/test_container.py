from common_fixtures import *
import time
import random
import re


def test_container_create_only(client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = client.create_container(name="test",
                                        imageUuid=uuid,
                                        startOnCreate=False)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "creating",
        "imageUuid": uuid,
        "imageId": NOT_NONE,
    })

    container = wait_success(client, container)

    assert_fields(container, {
        "type": "container",
        "allocationState": "inactive",
        "state": "stopped",
        "imageUuid": uuid,
        "imageId": NOT_NONE,
    })

    image = container.image()
    image = wait_success(client, image)
    assert_fields(image, {
        "state": "inactive"
    })

    volumes = container.volumes()
    assert len(volumes) == 1

    root_volume = wait_success(client, volumes[0])
    assert_fields(root_volume, {
        "allocationState": "inactive",
        "attachedState": "active",
        "state": "inactive",
        "instanceId": container.id,
        "deviceNumber": 0,
    })

    volume_mappings = root_volume.volumeStoragePoolMaps()
    assert len(volume_mappings) == 0

    nics = container.nics()
    assert len(nics) == 0

    image = wait_success(client, client.list_image(uuid=uuid)[0])
    assert_fields(image, {
        "state": "inactive",
        "uuid": uuid,
        "isPublic": True,
    })
    image_mappings = image.imageStoragePoolMaps()

    assert len(image_mappings) == 1

    image_mapping = wait_success(client, image_mappings[0])
    assert_fields(image_mapping, {
       "imageId": image.id,
       "storagePoolId": sim_context["external_pool"].id,
       "state": "inactive",
    })

    return container


def test_container_create_then_start(client, sim_context):
    container = test_container_create_only(client, sim_context)
    container = container.start()

    assert_fields(container, {
        "state": "starting"
    })

    container = wait_success(client, container)

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


@pytest.mark.skipif("True")
def test_container_stop(client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = client.create_container(name="test",
                                        imageUuid=uuid,
                                        startOnCreate=True)
    container = wait_success(client, container)

    assert_fields(container, {
        "state": "running"
    })

    container = container.stop()

    assert_fields(container, {
        "state": "stopping"
    })

    container = wait_success(client, container)

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
    assert len(instance_host_mappings) == 0
