import logging

from dstack import default_value, Config
from dstack.utils import memoize

log = logging.getLogger('docker')

_ENABLED = True


class DockerConfig:
    def __init__(self):
        pass

    @staticmethod
    def docker_enabled():
        return default_value('DOCKER_ENABLED', 'true') == 'true'

    @staticmethod
    def docker_host_ip():
        return default_value('DOCKER_HOST_IP', Config.agent_ip())

    @staticmethod
    def docker_home():
        return default_value('DOCKER_HOME', '/var/lib/docker')

    @staticmethod
    def docker_uuid_file():
        def_value = '{0}/.docker_uuid'.format(Config.home())
        return default_value('DOCKER_UUID_FILE', def_value)

    @staticmethod
    @memoize
    def docker_uuid():
        return Config.get_uuid_from_file('DOCKER_UUID',
                                         DockerConfig.docker_uuid_file())

    @staticmethod
    def url_base():
        return default_value('DOCKER_URL_BASE', None)

    @staticmethod
    def api_version():
        return default_value('DOCKER_API_VERSION', '1.8')


def docker_client():
    return Client(base_url=DockerConfig.url_base(),
                  version=DockerConfig.api_version())

from .storage import DockerPool
from .compute import DockerCompute
from dstack import type_manager

try:
    from docker import Client
except:
    log.info('Disabling docker, docker-py not found')
    _ENABLED = False

try:
    if _ENABLED:
        docker_client().info()
except Exception, e:
    log.info('Disabling docker, could not contact docker')
    _ENABLED = False

if _ENABLED and DockerConfig.docker_enabled():
    type_manager.register_type(type_manager.STORAGE_DRIVER, DockerPool())
    type_manager.register_type(type_manager.COMPUTE_DRIVER, DockerCompute())
