import logging
from dstack.storage import BaseStoragePool
from dstack.agent.handler import KindBasedMixin
from . import docker_client


log = logging.getLogger("docker")


class DockerPool(KindBasedMixin, BaseStoragePool):
    def __init__(self):
        KindBasedMixin.__init__(self, kind="docker")
        BaseStoragePool.__init__(self)

    def _get_image_by_id(self, id):
        templates = docker_client().images(all=True)
        templates = filter(lambda x: x["Id"] == id, templates)

        if len(templates) > 0:
            return templates[0]
        return None

    def _is_image_active(self, image, storage_pool):
        return self._get_image_by_id(image.data.dockerImage.id) is not None

    def _do_image_activate(self, image, storage_pool, progress):
        client = docker_client()
        data = image.data.dockerImage
        for status in client.pull(repository=data.qualifiedName, tag=data.tag, stream=True):
            log.info("Pulling [%s] status : %s", data.fullName, status)
            progress.update(status)

    def _get_image_storage_pool_map_data(self, obj):
        image = self._get_image_by_id(obj.image.data.dockerImage.id)
        return {
            "+data": {
               "dockerImage": image
            }
        }

    def _is_volume_active(self, volume, storage_pool):
        if volume.deviceNumber == 0:
            return True
        return False

