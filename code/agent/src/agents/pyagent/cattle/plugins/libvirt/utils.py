from cattle.type_manager import get_type_list, register_type

from .connection import LibvirtConnection
from xml.etree import ElementTree

_LIBVIRT_POOL_DRIVER = 'LIBVIRT_POOL_DRIVER'
_LIBVIRT_VOLUME_DRIVER = 'LIBVIRT_VOLUME_DRIVER'

DATA_TAG = '{http://cattle.io/schemas/cattle-libvirt}data'
DATA_NAME = '{http://cattle.io/schemas/cattle-libvirt}name'


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


def read_vnc_info(xml_string):
    doc = ElementTree.fromstring(xml_string)

    vnc = None
    for child in doc.findall('devices/graphics'):
        if child.attrib['type'] == 'vnc':
            vnc = child
            break

    if vnc is None:
        return None, None, None

    passwd = None
    for child in doc.findall('metadata/{0}'.format(DATA_TAG)):
        if child.attrib[DATA_NAME] == 'vncpasswd':
            passwd = child.text

    if passwd is None:
        return None, None, None

    return vnc.attrib['listen'], vnc.attrib['port'], passwd
