import logging
from cattle.storage import BaseStoragePool
from cattle.agent.handler import KindBasedMixin
from . import docker_client, get_compute


log = logging.getLogger('docker')


class DockerPool(KindBasedMixin, BaseStoragePool):
    def __init__(self):
        KindBasedMixin.__init__(self, kind='docker')
        BaseStoragePool.__init__(self)

    @staticmethod
    def _get_image_by_id(id):
        templates = docker_client().images(all=True)
        templates = filter(lambda x: x['Id'] == id, templates)

        if len(templates) > 0:
            return templates[0]
        return None

    @staticmethod
    def _get_image_by_label(tag):
        templates = docker_client().images(all=True, name=tag.split(':', 1)[0])
        templates = filter(lambda x: tag in x['RepoTags'], templates)

        if len(templates) > 0:
            return templates[0]
        return None

    def pull_image(self, image, progress):
        if not self._is_image_active(image, None):
            self._do_image_activate(image, None, progress)

    def _is_image_active(self, image, storage_pool):
        image_obj = self._get_image_by_label(image.data.dockerImage.fullName)
        return image_obj is not None

    def _do_image_activate(self, image, storage_pool, progress):
        client = docker_client()
        data = image.data.dockerImage

        # TODO: Disable progress until 0.3.0+ is released, bug makes this fail
        progress = None

        if progress is None:
            client.pull(repository=data.qualifiedName, tag=data.tag)
        else:
            for status in client.pull(repository=data.qualifiedName,
                                      tag=data.tag,
                                      stream=True):
                log.info('Pulling [%s] status : %s', data.fullName, status)
                progress.update(status)

    def _get_image_storage_pool_map_data(self, obj):
        image = self._get_image_by_label(obj.image.data.dockerImage.fullName)
        return {
            '+data': {
                'dockerImage': image
            }
        }

    def _get_volume_storage_pool_map_data(self, obj):
        return {
            'volume': {
                'format': 'docker'
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

        container = get_compute().get_container_by_name(volume.instance.uuid)
        return container is None

    def _do_volume_remove(self, volume, storage_pool, progress):
        if volume.deviceNumber != 0:
            raise Exception("Non-root volumes are not supported")

        container = get_compute().get_container_by_name(volume.instance.uuid)
        if container is None:
            return

        docker_client().remove_container(container)
