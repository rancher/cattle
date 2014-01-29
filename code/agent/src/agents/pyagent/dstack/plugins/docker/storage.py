import logging
from dstack.storage import BaseStoragePool
from dstack.agent.handler import KindBasedMixin
from dstack.plugins.docker.compute import DockerCompute
from . import docker_client


log = logging.getLogger('docker')


class DockerPool(KindBasedMixin, BaseStoragePool):
    def __init__(self):
        self._compute = DockerCompute()
        KindBasedMixin.__init__(self, kind='docker')
        BaseStoragePool.__init__(self)

    @staticmethod
    def _get_image_by_id(id):
        templates = docker_client().images(all=True)
        templates = filter(lambda x: x['Id'] == id, templates)

        if len(templates) > 0:
            return templates[0]
        return None

    def _is_image_active(self, image, storage_pool):
        return self._get_image_by_id(image.data.dockerImage.id) is not None

    def _do_image_activate(self, image, storage_pool, progress):
        client = docker_client()
        data = image.data.dockerImage
        for status in client.pull(repository=data.qualifiedName, tag=data.tag,
                                  stream=True):
            log.info('Pulling [%s] status : %s', data.fullName, status)
            progress.update(status)

    def _get_image_storage_pool_map_data(self, obj):
        image = self._get_image_by_id(obj.image.data.dockerImage.id)
        return {
            'image': {
                '+data': {
                    'dockerImage': image
                }
            }
        }

    def _is_volume_active(self, volume, storage_pool):
        if volume.deviceNumber != 0:
            raise Exception("Non-root volumes are not supported")
        return True

    def _is_volume_inactive(self, volume, storage_pool):
        if volume.deviceNumber != 0:
            raise Exception("Non-root volumes are not supported")
        return True

    def _is_volume_removed(self, volume, storage_pool):
        if volume.deviceNumber != 0:
            raise Exception("Non-root volumes are not supported")

        container = self._compute.get_container_by_name(volume.instance.uuid)
        return container is None

    def _do_volume_remove(self, volume, storage_pool, progress):
        if volume.deviceNumber != 0:
            raise Exception("Non-root volumes are not supported")

        container = self._compute.get_container_by_name(volume.instance.uuid)
        if container is None:
            return

        docker_client().remove_container(container)
