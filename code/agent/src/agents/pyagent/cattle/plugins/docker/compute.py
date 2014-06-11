import logging
import requests.exceptions
import time

from . import docker_client, pull_image
from . import DockerConfig, DOCKER_COMPUTE_LISTENER
from cattle import Config
from cattle.compute import BaseComputeDriver
from cattle.agent.handler import KindBasedMixin
from cattle.type_manager import get_type_list
from cattle import utils
from docker import APIError

log = logging.getLogger('docker')


def _is_running(container):
    if container is None:
        return False

    client = docker_client()
    inspect = client.inspect_container(container)

    try:
        return inspect['State']['Running']
    except KeyError:
        return False


def _is_stopped(container):
    return not _is_running(container)


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

    def on_ping(self, ping, pong):
        if not DockerConfig.docker_enabled():
            return

        self._add_resources(ping, pong)
        self._add_instances(ping, pong)

    def _add_instances(self, ping, pong):
        if not utils.ping_include_instances(ping):
            return

        containers = []
        for c in docker_client().containers():
            names = c.get('Names', [])
            for name in names:
                if name.startswith('/'):
                    name = name[1:]
                    if utils.is_uuid(name):
                        containers.append({
                            'type': 'instance',
                            'uuid': name,
                            'state': 'running'
                        })

        utils.ping_add_resources(pong, *containers)
        utils.ping_set_option(pong, 'instances', True)

    def _add_resources(self, ping, pong):
        if not utils.ping_include_resources(ping):
            return

        physical_host = Config.physical_host()

        compute = {
            'type': 'host',
            'kind': 'docker',
            'name': Config.hostname() + '/docker',
            'physicalHostUuid': physical_host['uuid'],
            'uuid': DockerConfig.docker_uuid()
        }

        pool = {
            'type': 'storagePool',
            'kind': 'docker',
            'name': compute['name'] + ' Storage Pool',
            'hostUuid': compute['uuid'],
            'uuid': compute['uuid'] + '-pool'
        }

        utils.ping_add_resources(pong, physical_host, compute, pool)

    def inspect(self, container):
        return docker_client().inspect_container(container)

    def get_container_by_name(self, name):
        name = '/{0}'.format(name)
        return self.get_container_by(lambda x: name in x.get('Names', []))

    def _is_instance_active(self, instance, host):
        container = self.get_container_by_name(instance.uuid)
        return _is_running(container)

    @staticmethod
    def _setup_command(config, instance):
        command = ""
        try:
            command = instance.data.fields.command
        except (KeyError, AttributeError):
            return None

        if len(command.strip()) == 0:
            return None

        command_args = []
        try:
            command_args = instance.data.fields.commandArgs
        except (KeyError, AttributeError):
            pass

        if len(command_args) > 0:
            command = [command]
            command.extend(command_args)

        if command is not None:
            config['command'] = command

    @staticmethod
    def _setup_links(start_config, instance):
        links = {}

        if 'instanceLinks' not in instance:
            return

        for link in instance.instanceLinks:
            if link.targetInstanceId is not None:
                links[link.targetInstance.uuid] = link.linkName

        start_config['links'] = links

    @staticmethod
    def _setup_ports(config, instance):
        ports = []
        try:
            for port in instance.ports:
                ports.append((port.privatePort, port.protocol))
        except (AttributeError, KeyError):
            pass

        if len(ports) > 0:
            config['ports'] = ports

    def _do_instance_activate(self, instance, host, progress):
        name = instance.uuid
        try:
            image_tag = instance.image.data.dockerImage.fullName
        except KeyError:
            raise Exception('Can not start container with no image')

        c = docker_client()

        config = {
            'name': name,
            'detach': True
        }

        # Docker-py doesn't support working_dir, maybe in 0.2.4?
        copy_fields = [
            ('environment', 'environment'),
            ('hostname', 'hostname'),
            ('user', 'user')]

        for src, dest in copy_fields:
            try:
                config[dest] = instance.data.fields[src]
            except (KeyError, AttributeError):
                pass

        self._setup_command(config, instance)
        self._setup_ports(config, instance)

        start_config = {
            'publish_all_ports': True,
            'privileged': self._is_privileged(instance)
        }

        self._setup_links(start_config, instance)

        self._call_listeners(True, instance, host, config, start_config)

        container = self.get_container_by_name(name)
        if container is None:
            log.info('Creating docker container [%s] from config %s', name,
                     config)

            try:
                container = c.create_container(image_tag, **config)
            except APIError as e:
                try:
                    if e.message.response.status_code == 404:
                        # Ensure image is pulled, somebody could have deleted
                        # it behind the scenes
                        pull_image(instance.image, progress)
                        container = c.create_container(image_tag, **config)
                    else:
                        raise(e)
                except:
                    raise(e)

        log.info('Starting docker container [%s] docker id [%s] %s', name,
                 container['Id'], start_config)
        c.start(container['Id'], **start_config)

        self._call_listeners(False, instance, host, container['Id'])

    def _call_listeners(self, before, *args):
        for listener in get_type_list(DOCKER_COMPUTE_LISTENER):
            if before:
                listener.before_start(*args)
            else:
                listener.after_start(*args)

    def _is_privileged(self, instance):
        try:
            return instance.data.fields['privileged']
        except (KeyError, AttributeError):
            return False

    def _get_instance_host_map_data(self, obj):
        existing = self.get_container_by_name(obj.instance.uuid)
        docker_ports = {}
        docker_ip = None

        if existing is not None:
            inspect = docker_client().inspect_container(existing['Id'])
            docker_ip = inspect['NetworkSettings']['IPAddress']
            if existing.get('Ports') is not None:
                for port in existing['Ports']:
                    if 'PublicPort' in port and 'PrivatePort' not in port:
                        # Remove after docker 0.12/1.0 is released
                        private_port = '{0}/{1}'.format(port['PublicPort'],
                                                        port['Type'])
                        docker_ports[private_port] = None
                    elif 'PublicPort' in port:
                        private_port = '{0}/{1}'.format(port['PrivatePort'],
                                                        port['Type'])
                        docker_ports[private_port] = str(port['PublicPort'])
                    else:
                        private_port = '{0}/{1}'.format(port['PrivatePort'],
                                                        port['Type'])
                        docker_ports[private_port] = None

        return {
            'instance': {
                '+data': {
                    'dockerContainer': existing,
                    '+fields': {
                        'dockerHostIp': DockerConfig.docker_host_ip(),
                        'dockerPorts': docker_ports,
                        'dockerIp': docker_ip
                    }
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

        start = time.time()
        while True:
            try:
                c.stop(container['Id'], timeout=1)
                break
            except requests.exceptions.Timeout:
                if (time.time() - start) > Config.stop_timeout():
                    break

        container = self.get_container_by_name(name)
        if not _is_stopped(container):
            c.kill(container['Id'])

        container = self.get_container_by_name(name)
        if not _is_stopped(container):
            raise Exception('Failed to stop container for VM [{0}]'
                            .format(name))
