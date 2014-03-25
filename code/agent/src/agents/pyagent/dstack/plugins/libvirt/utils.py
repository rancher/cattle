from dstack.type_manager import get_type_list, register_type

from .connection import LibvirtConnection
from xml.etree import ElementTree

_LIBVIRT_POOL_DRIVER = 'LIBVIRT_POOL_DRIVER'
_LIBVIRT_VOLUME_DRIVER = 'LIBVIRT_VOLUME_DRIVER'


def register_volume_driver(driver):
    register_type(_LIBVIRT_VOLUME_DRIVER, driver)


def register_pool_driver(driver):
    register_type(_LIBVIRT_POOL_DRIVER, driver)


def volume_drivers():
    return get_type_list(_LIBVIRT_VOLUME_DRIVER)


def pool_drivers():
    return get_type_list(_LIBVIRT_POOL_DRIVER)


def get_preferred_libvirt_type():
    conn = LibvirtConnection.open('qemu')
    caps = ElementTree.fromstring(conn.getCapabilities())

    result = set()

    for cap in caps.findall(".//domain"):
        result.add(cap.get('type', default=''))

    for i in ['kvm', 'qemu']:
        if i in result:
            return i

    return None
