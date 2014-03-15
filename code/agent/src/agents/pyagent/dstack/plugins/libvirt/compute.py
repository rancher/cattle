import logging
import os

from . import LIBVIRT_KIND
from .connection import LibvirtConnection
from .storage import get_pool_driver
from .utils import pool_drivers
from .config import LibvirtConfig
from dstack import Config
from dstack.compute import BaseComputeDriver
from dstack.agent.handler import KindBasedMixin
from dstack import utils

from mako.template import Template
from mako.lookup import TemplateLookup

DEFAULT_CONFIG_PATHS = [
    ['host', 'data', 'libvirt'],
    ['instance', 'data', 'libvirt'],
    ['instance', 'offering', 'data', 'libvirt'],
]

log = logging.getLogger('libvirt-compute')


def _is_running(conn, instance):
    if instance is None:
        return False
    return True


class TemplateConfig(object):
    def __init__(self, instance, host, paths=DEFAULT_CONFIG_PATHS):
        self.instance = instance
        self.host = host
        self.paths = paths

    def param(self, name, default=None):
        for path in self.paths:
            do_continue = False
            src = self

            for part in path:
                if src is None:
                    do_continue = True
                    break
                else:
                    try:
                        src = getattr(src, part)
                    except AttributeError:
                        try:
                            src = src[part]
                        except KeyError:
                            do_continue = True
                            break

            if do_continue:
                continue

            try:
                if src is not None:
                    return src[name]
            except KeyError:
                pass

        return default


class LibvirtCompute(KindBasedMixin, BaseComputeDriver):
    def __init__(self):
        KindBasedMixin.__init__(self, kind='libvirt')
        BaseComputeDriver.__init__(self)

    @staticmethod
    def get_instance_by(conn, func):
        containers = filter(func, conn.listAllDomains())

        if len(containers) > 0:
            return containers[0]

        return None

    @staticmethod
    def on_ping(ping, pong):
        if not utils.ping_include_resources(ping):
            return

        compute = {
            'type': 'host',
            'kind': LIBVIRT_KIND,
            'name': Config.hostname() + '/libvirt',
            'uuid': LibvirtConfig.libvirt_uuid()
        }

        resources = [compute]

        for driver in pool_drivers():
            for pool in driver.discover(compute):
                data = utils.get_data(pool, 'data', 'libvirt')
                data['driver'] = driver.driver_name()

                resources.append(pool)

        utils.ping_add_resources(pong, *resources)

    def get_instance_by_uuid(self, conn, uuid):
        return self.get_instance_by(conn, lambda x: uuid in x['Names'])

    def _is_instance_active(self, instance, host):
        conn = LibvirtConnection.open()
        instance = self.get_instance_by_uuid(conn, instance.uuid)
        return _is_running(conn, instance)

    def _get_template(self, instance, host):
        template = LibvirtConfig.default_template_name()

        try:
            template = instance.offering.data.libvirt.template
        except AttributeError:
            pass

        try:
            template = instance.data.libvirt.template
        except AttributeError:
            pass

        dirs = LibvirtConfig.template_dirs()
        for template_dir in dirs:
            full_path = os.path.join(template_dir, template)
            if os.path.exists(full_path):
                return Template(filename=full_path,
                                lookup=TemplateLookup(directories=dirs))

        raise Exception('Failed to find template [{0}]'.format(template))

    def _get_volumes(self, instance):
        ret = []

        for volume in instance.volumes:
            if len(volume.storagePools) > 0:
                storage_pool = volume.storagePools[0]
                driver = get_pool_driver(storage_pool)
                volume_obj = driver.get_volume(volume, storage_pool)
                ret.append(volume_obj)

        return ret

    def _do_instance_activate(self, instance, host, progress):
        template = self._get_template(instance, host)
        config = TemplateConfig(instance, host)
        output = template.render(instance=instance,
                                 volumes=self._get_volumes(instance),
                                 host=host,
                                 config=config)

        conn = LibvirtConnection.open()
        dom = conn.createXML(output, 0)
        print output
        print dom

    def _get_instance_host_map_data(self, obj):
        existing = self.get_instance_by_name(obj.instance.uuid)
        docker_ports = {}
        docker_ip = None

        if existing is not None:
            inspect = docker_client().inspect_container(existing['Id'])
            docker_ip = inspect['NetworkSettings']['IPAddress']
            if existing.get('Ports') is not None:
                for port in existing['Ports']:
                    private_port = '{0}/{1}'.format(port['PrivatePort'],
                                                    port['Type'])
                    docker_ports[private_port] = str(port['PublicPort'])

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
