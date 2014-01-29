def docker_client():
    return Client()

from .storage import DockerPool
from .compute import DockerCompute
from dstack import type_manager
from docker import Client


type_manager.register_type(type_manager.STORAGE_DRIVER, DockerPool())
type_manager.register_type(type_manager.COMPUTE_DRIVER, DockerCompute())
