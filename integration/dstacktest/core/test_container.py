from common_fixtures import *
import time
import random
import re


def test_container_create(client, sim_context):
    uuid = "sim:{}".format(random_num())
    container = client.create_container(name="test",
                                        imageUuid=uuid,
                                        startOnCreate=False)

    container = wait_success(client, container)

    assert container is not None
    assert "container" == container.type
    assert container.allocationState == "inactive"
    assert container.state == "stopped"
    assert container.imageUuid == uuid
    assert container.imageId is not None

    image = container.image()
    image = wait_success(client, image)
    assert image.state == "inactive"

    volumes = container.volumes()
    assert len(volumes) == 1

    root_volume = volumes[0]
    root_volume = wait_success(client, root_volume)
    assert root_volume.allocationState == "inactive"
    assert root_volume.attachedState == "active"
    assert root_volume.state == "inactive"
    assert root_volume.instanceId == container.id
    assert root_volume.deviceNumber == 0


    nics = container.nics()
    assert len(nics) == 0

    image = client.list_image(uuid=uuid)[0]
    image_mappings = image.imageStoragePoolMaps()

    assert len(image_mappings) == 1

    image_mapping = wait_success(client, image_mappings[0])

    assert image_mapping.imageId == image.id
    assert image_mapping.storagePoolId == sim_context["external_pool"].id
    assert image_mapping.state == "inactive"
    assert image.isPublic
    assert image.uuid == uuid

    return container


def test_container_create_then_start(client, sim_context):
    container = test_container_create(client, sim_context)
    container = container.start()

    assert container.state == "starting"

    container = wait_success(client, container)

    assert container.allocationState == "active"
    assert container.state == "running"
    root_volume = container.volumes()[0]

    assert root_volume.state == "active"

    image = root_volume.image()

    assert image is not None
    assert image.state == "active"

