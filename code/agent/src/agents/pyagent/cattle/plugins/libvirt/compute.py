import logging
import os

from . import LIBVIRT_KIND
from .connection import LibvirtConnection
from .storage import get_pool_driver
from .utils import pool_drivers, get_preferred_libvirt_type, read_vnc_info
from .config import LibvirtConfig
from cattle import Config
from cattle.compute import BaseComputeDriver
from cattle.agent.handler import KindBasedMixin
from cattle import utils

from mako.template import Template
from mako.lookup import TemplateLookup

DEFAULT_CONFIG_PATHS = [
    ['instance', 'data', 'libvirt'],
    ['instance', 'template', 'data', 'libvirt'],
    ['instance', 'offering', 'data', 'libvirt'],
    ['host', 'data', 'libvirt'],
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
                if src[name] is not None:
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
        containers = filter(func, conn.listAllDomains(0))

        if len(containers) > 0:
            return containers[0]

        return None

    def on_ping(self, ping, pong):
        if not utils.ping_include_resources(ping):
            return

        physical_host = Config.physical_host()

        compute = {
            'type': 'host',
            'kind': LIBVIRT_KIND,
            'name': Config.hostname() + '/libvirt',
            'uuid': LibvirtConfig.libvirt_uuid(),
            'physicalHostUuid': physical_host['uuid'],
            'data': {
                'libvirt': {
                    'type': get_preferred_libvirt_type()
                }
            }
        }

        resources = [physical_host, compute]

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
    def _get_template(config):
        defaults = LibvirtConfig.default_template_names()
        for default_template in defaults:
            template = config.param('template', default_template)

            dirs = LibvirtConfig.template_dirs()
            for template_dir in dirs:
                if os.path.exists(template):
                    full_path = template
                    uri = None
                else:
                    full_path = os.path.join(template_dir, template)
                    uri = '/' + template

                if os.path.exists(full_path):
                    return Template(filename=full_path,
                                    uri=uri,
                                    lookup=TemplateLookup(directories=dirs))

        raise Exception('Failed to find template {0}'.format(defaults))

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
        config = InstanceConfig(instance, host)
        template = self._get_template(config)
        default_network = config.param('defaultNetwork', {
            'type': 'network',
            'source': [
                {'network': 'default'}
            ]
        })

        if not isinstance(default_network, dict):
            interfaces = []
        else:
            interfaces = [default_network]

        output = template.render(instance=instance,
                                 volumes=self._get_volumes(instance),
                                 interfaces=interfaces,
                                 host=host,
                                 config=config,
                                 randomToken=utils.random_string())

        conn = self._get_connection(instance, host)
        log.info('Starting %s : XML %s', instance.uuid, output)
        conn.createXML(output, 0)

    def _get_connection(self, instance, host):
        type = InstanceConfig(instance, host).param('type', 'kvm')
        return LibvirtConnection.open(type)

    def _get_instance_host_map_data(self, obj):
        conn = self._get_connection(obj.instance, obj.host)
        existing = self.get_instance_by_uuid(conn, obj.instance.uuid)

        if existing is None:
            return {
                'instance': {
                    '+data': {
                        '+fields': {
                            'libvirtVncAddress': None,
                            'libvirtVncPassword': None
                        }
                    }
                }
            }

        xml = existing.XMLDesc(0)
        host, port, passwd = read_vnc_info(xml)

        if host == '0.0.0.0':
            host = LibvirtConfig.host_ip()

        if host is None:
            vnc_address = None
            vnc_passwd = None
        else:
            vnc_address = '{0}:{1}'.format(host, port)
            vnc_passwd = passwd

        return {
            'instance': {
                '+data': {
                    '+libvirt': {
                        'xml': existing.XMLDesc(0)
                    },
                    '+fields': {
                        'libvirtVncAddress': vnc_address,
                        'libvirtVncPassword': vnc_passwd
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
