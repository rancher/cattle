import logging

from . import docker_client
from dstack.compute import BaseComputeDriver
from dstack.agent.handler import KindBasedMixin

log = logging.getLogger("docker")


class DockerCompute(KindBasedMixin, BaseComputeDriver):
    def __init__(self):
        KindBasedMixin.__init__(self, kind="docker")
        BaseComputeDriver.__init__(self)

    def _get_container_by(self, func):
        c = docker_client()
        containers = c.containers(all=True,trunc=False)
        containers = filter(func, containers)

        if len(containers) > 0:
            return containers[0]

        return None

    def _get_container_by_name(self, name):
        name = "/{0}".format(name)
        return self._get_container_by(lambda x: name in x["Names"])

    def _is_instance_active(self, instance, host):
        container = self._get_container_by_name(instance.uuid)
        return container is not None and container["Status"].startswith("Up")

    def _do_instance_activate(self, instance, host, progress):
        name = instance.uuid
        image = instance.image.data.dockerImage.id
        c = docker_client()

        existing = self._get_container_by_name(name)
        if existing is not None:
            c.remove_container(existing["Id"])

        config = {
            "name": name,
        }

        log.info("Creating docker container [%s] from config %s", name, config)
        container = c.create_container(image, **config)
        log.info("Starting docker container [%s] docker id [%s]", name, container["Id"])
        c.start(container["Id"])

    def _get_instance_host_map_data(self, obj):
        existing = self._get_container_by_name(obj.instance.uuid)
        return {
            "+data": {
                "dockerContainer": existing
            }
        }
