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


class InstanceConfig(object):
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

    def on_ping(self, ping, pong):
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
                data = utils.get_map_value(pool, 'data', 'libvirt')
                data['driver'] = driver.driver_name()

                resources.append(pool)

        utils.ping_add_resources(pong, *resources)

    def get_instance_by_uuid(self, conn, uuid):
        return self.get_instance_by(conn, lambda x: x.name() == uuid)

    def _is_instance_active(self, instance, host):
        conn = self._get_connection(instance, host)
        instance = self.get_instance_by_uuid(conn, instance.uuid)
        return _is_running(conn, instance)

    @staticmethod
    def _get_template(instance, host):
        template = LibvirtConfig.default_template_name()

        try:
            template = host.data.libvirt.template
        except AttributeError:
            pass

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

    @staticmethod
    def _get_volumes(instance):
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
        config = InstanceConfig(instance, host)
        output = template.render(instance=instance,
                                 volumes=self._get_volumes(instance),
                                 host=host,
                                 config=config)

        conn = self._get_connection(instance, host)
        log.info('Starting %s', instance.uuid)
        conn.createXML(output, 0)

    def _get_connection(self, instance, host):
        type = InstanceConfig(instance, host).param('type', 'kvm')
        return LibvirtConnection.open(type)

    def _get_instance_host_map_data(self, obj):
        conn = self._get_connection(obj.instance, obj.host)
        existing = self.get_instance_by_uuid(conn, obj.instance.uuid)

        if existing is None:
            return {}
        else:
            return {
                'instance': {
                    '+data': {
                        '+libvirt': {
                            'xml': existing.XMLDesc()
                        }
                    }
                }
            }

    def _is_instance_inactive(self, instance, host):
        conn = self._get_connection(instance, host)
        vm = self.get_instance_by_uuid(conn, instance.uuid)

        return vm is None

    def _do_instance_deactivate(self, instance, host, progress):
        conn = self._get_connection(instance, host)
        vm = self.get_instance_by_uuid(conn, instance.uuid)

        if vm is None:
            return

        try:
            log.info('Stopping %s', instance.uuid)
            vm.shutdown()
        except:
            pass
        finally:
            # TODO Wait for shutdown a bit before destroying
            log.info('Destroy %s', instance.uuid)
            vm.destroy()
