from os import path
from cattle import default_value, Config, CONFIG_OVERRIDE

import os
import re


class LibvirtConfig:
    def __init__(self):
        pass

    @staticmethod
    def pool_drivers():
        return default_value('LIBVIRT_POOL_DRIVERS', 'directory').split()

    @staticmethod
    def pool_directories():
        return default_value('LIBVIRT_POOL_DIRECTORIES',
                             path.join(Config.home(), 'pools/libvirt')).split()

    @staticmethod
    def libvirt_uuid_file():
        def_value = '{0}/.libvirt_uuid'.format(Config.home())
        return default_value('LIBVIRT_UUID_FILE', def_value)

    @staticmethod
    def libvirt_uuid():
        return Config.get_uuid_from_file('LIBVIRT_UUID',
                                         LibvirtConfig.libvirt_uuid_file())

    @staticmethod
    def template_dirs():
        default = path.join(path.dirname(__file__))
        return default_value('LIBVIRT_TEMPLATE_DIR', default).split()

    @staticmethod
    def default_template_names():
        value = default_value('LIBVIRT_DEFAULT_TEMPLATE',
                              'custom_template.tmpl, default_template.tmpl')
        return re.split(r'\s*,\s*', value)

    @staticmethod
    def host_ip():
        return default_value('LIBVIRT_HOST_IP', Config.agent_ip())

    @staticmethod
    def console_enabled():
        val = default_value('LIBVIRT_CONSOLE', 'false')
        return val in ['true', 'True']

    @staticmethod
    def set_console_enabled(val):
        CONFIG_OVERRIDE['LIBVIRT_CONSOLE'] = str(val)

    @staticmethod
    def websockify_session_dir():
        return default_value('LIBVIRT_WEBSOCKIFY_DIR',
                             os.path.join(Config.home(), 'websockify',
                                          'session'))

    @staticmethod
    def websockify_listen_port():
        return default_value('LIBVIRT_WEBSOCKIFY_LISTEN_PORT', 9343)

    @staticmethod
    def websockify_listen_host():
        return default_value('LIBVIRT_WEBSOCKIFY_LISTEN_HOST',
                             LibvirtConfig.host_ip())

    @staticmethod
    def websockify_session_timeout():
        return int(default_value('LIBVIRT_WEBSOCKIFY_SESSION_TIMEOUT', '300'))

    @staticmethod
    def websockify_enabled():
        return default_value('LIBVIRT_WEBSOCKIFY_ENABLED', 'true') == 'true'

    @staticmethod
    def libvirt_required():
        return default_value('LIBVIRT_REQUIRED', False) == 'true'
