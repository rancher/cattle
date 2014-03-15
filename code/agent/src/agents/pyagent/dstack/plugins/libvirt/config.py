from os import path
from dstack import default_value, Config
from dstack.utils import memoize


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
    @memoize
    def libvirt_uuid():
        return Config.get_uuid_from_file('LIBVIRT_UUID',
                                         LibvirtConfig.libvirt_uuid_file())

    @staticmethod
    def template_dirs():
        default = path.join(path.dirname(__file__))
        return default_value('LIBVIRT_TEMPLATE_DIR', default).split()

    @staticmethod
    def default_template_name():
        return default_value('LIBVIRT_DEFAULT_TEMPLATE', 'default_template.tmpl')
