from .common_fixtures import *
import time
import random
import re


def test_container_create(client, sim_external_pool):
    uuid = "sim:{}".format(random_num())
    container = client.create_container(name="test", imageUuid=uuid)
    container = wait_success(client, container)

    assert container is not None
    assert "container" == container.type

    image = client.list_image(uuid=uuid)[0]
    image_mapping = image.imageStoragePoolMaps()

    assert len(image_mapping) == 1
    assert image_mapping[0].imageId == image.id
    assert image_mapping[0].storagePoolId == sim_external_pool.id
    assert image.isPublic
    assert image.uuid == uuid
