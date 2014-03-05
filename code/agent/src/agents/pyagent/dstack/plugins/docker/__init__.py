import logging
from uuid import uuid4
from os import path

from dstack import default_value, Config
from dstack.utils import memoize


def docker_client():
    return Client()

log = logging.getLogger('docker')

ENABLED = True


class DockerConfig:
    def __init__(self):
        pass

    @staticmethod
    def docker_host_ip():
        return default_value('DOCKER_HOST_IP', Config.agent_ip())

    @staticmethod
    def docker_home():
        return default_value('DOCKER_HOME', '/var/lib/docker')

    @staticmethod
    def docker_uuid_file():
        def_value = '{0}/.dstack_uuid'.format(DockerConfig.docker_home())
        return default_value('DOCKER_UUID_FILE', def_value)

    @staticmethod
    @memoize
    def docker_uuid():
        uuid = default_value('DOCKER_UUID', None)
        if uuid is not None:
            return uuid

        uuid_file = DockerConfig.docker_uuid_file()
        if path.exists(uuid_file):
            with open(uuid_file) as f:
                uuid = f.read().strip()
            if len(uuid) == 0:
                uuid = None

        if uuid is None:
            uuid = str(uuid4())
            with open(uuid_file, 'w') as f:
                f.write(uuid)

        return uuid

from .storage import DockerPool
from .compute import DockerCompute
from dstack import type_manager

try:
    from docker import Client
except:
    log.info('Disabling docker, docker-py not found')
    ENABLED = False

try:
    if ENABLED:
        docker_client().info()
except Exception, e:
    log.info('Disabling docker, could not contact docker')
    ENABLED = False

type_manager.register_type(type_manager.STORAGE_DRIVER, DockerPool())
type_manager.register_type(type_manager.COMPUTE_DRIVER, DockerCompute())
