import logging
import requests.exceptions

from . import docker_client
from dstack import Config
from dstack.compute import BaseComputeDriver
from dstack.agent.handler import KindBasedMixin

log = logging.getLogger('docker')


def _is_running(container):
    return container is not None and container['Status'].startswith('Up')


def _is_stopped(container):
    return container is None or container['Status'].startswith('Exit')


class DockerCompute(KindBasedMixin, BaseComputeDriver):
    def __init__(self):
        KindBasedMixin.__init__(self, kind='docker')
        BaseComputeDriver.__init__(self)

    @staticmethod
    def get_container_by(func):
        c = docker_client()
        containers = c.containers(all=True, trunc=False)
        containers = filter(func, containers)

        if len(containers) > 0:
            return containers[0]

        return None

    def get_container_by_name(self, name):
        name = '/{0}'.format(name)
        return self.get_container_by(lambda x: name in x['Names'])

    def _is_instance_active(self, instance, host):
        container = self.get_container_by_name(instance.uuid)
        return _is_running(container)

    def _do_instance_activate(self, instance, host, progress):
        name = instance.uuid
        image = instance.image.data.dockerImage.id
        c = docker_client()

        config = {
            'name': name,
        }

        container = self.get_container_by_name(name)
        if container is None:
            log.info('Creating docker container [%s] from config %s', name,
                     config)
            container = c.create_container(image, **config)
        log.info('Starting docker container [%s] docker id [%s]', name,
                 container['Id'])
        c.start(container['Id'])

    def _get_instance_host_map_data(self, obj):
        existing = self.get_container_by_name(obj.instance.uuid)
        return {
            'instance': {
                '+data': {
                    'dockerContainer': existing
                }
            }
        }

    def _is_instance_inactive(self, instance, host):
        name = instance.uuid
        container = self.get_container_by_name(name)

        return _is_stopped(container)

    def _do_instance_deactivate(self, instance, host, progress):
        name = instance.uuid
        c = docker_client()

        container = self.get_container_by_name(name)

        try:
            c.stop(container['Id'], timeout=Config.stop_timeout())
            return
        except requests.exceptions.Timeout:
            pass

        container = self.get_container_by_name(name)
        if not _is_stopped(container):
            c.kill(container['Id'])

        container = self.get_container_by_name(name)
        if not _is_stopped(container):
            raise Exception('Failed to stop container for VM [{0}]'
                            .format(name))
