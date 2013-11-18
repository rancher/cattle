import dstack.docker
from dstack import log
from dstack import utils
from dstack.storage import BaseStoragePool
from dstack.compute import BaseComputeDriver
from docker import Client
import json


class Container(object):
    def __init__(self, client, container=None):
        """
        @type client: docker.Client
        @type container: dict
        """
        self._client = client
        self._container = container


    @staticmethod
    def from_virtual_machine(virtual_machine):
        """
        @type virtual_machine: dict
        @return: Container
        """
        container = utils.get_data(virtual_machine, prefix="docker.container")
        return Container(dstack.docker.get_client(), container)


    def exists(self):
        return not self._get_container() is None


    def create(self, start=True):
        log.info("Creating from config " + str(self._container))
        self._container = self._client.create_container_from_config(self._container)
        self._container = self._get_container(prefix=True)
        if start:
            self.start()
        return self._container


    def start(self):
        self._client.start(self._container.get("Id"))
        self._container = self._get_container()
        return self._container

    def stop(self):
        self._client.stop(self._container.get("Id"))
        self._container = self._get_container()
        return self._container

    def _get_container(self, id=None, prefix=False):
        if id is None:
            id = self._container.get("Id")

        if id is None:
            return None

        all_containers = self._client.containers(all=True)
        containers = filter(lambda x: x.get("Id") == id, all_containers)
        if len(containers) == 0 and prefix:
            containers = filter(lambda x: x.get("Id").startswith(id), all_containers)

        if len(containers) > 2:
            log.error("Multiple containers returned for id [%s] : %s" % (id, containers))

        if len(containers) == 0:
            return None

        return containers[0]



class DockerCompute(BaseComputeDriver):
    def start(self, instance=None, **kw):
        container = Container.from_virtual_machine(instance)

        if container.exists():
            log.info("Starting container")
            return container.start()
        else:
            log.info("Creating container")
            return container.create()

