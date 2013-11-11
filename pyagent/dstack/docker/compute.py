import dstack.docker
from dstack import log
from dstack import utils
from dstack.storage import BaseStoragePool
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


    def create(self):
        self._container = self._client.create_container_from_config(self._container)


    def start(self):
        self._client.start(self._container.get("Id"))
        self._container = self._get_container()

    def _get_container(self, id=None):
        if id is None:
            id = self._lookup_data()

        if id is None:
            return None

        containers = filter(lambda x: x.get("Id") == id, self._client.containers(all=True))

        if len(containers) > 2:
            log.error("Multiple containers returned for id [%s] : %s" % (id, containers))

        if len(containers) == 0:
            return None

        return containers[0]

    def _lookup_data(self):
        container = utils.get_data(self._container, prefix="docker.container")
        return container.get("Id")


class DockerCompute(object):
    def start(self, virtual_machine=None, **kw):
        container = Container.from_virtual_machine(virtual_machine)

        if container.exists():
            log.info("Starting container")
            container.start()
        else:
            log.info("Creating container")
            container.create()

