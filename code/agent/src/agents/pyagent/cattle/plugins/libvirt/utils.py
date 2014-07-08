import os
import subprocess

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


def _get_hvm_type():
    if os.path.exists('/proc/cpuinfo') and os.path.exists('/dev/kvm'):
        for line in open('/proc/cpuinfo'):
            parts = line.split()
            if len(parts) and parts[0] == 'flags':
                if 'svm' in parts:
                    return 'svm'
                if 'vmx' in parts:
                    return 'vmx'
    return None


def _kernel_mode_loaded():
    type = _get_hvm_type()
    mod = None
    if type == 'svm':
        mod = 'kvm_amd'
    elif type == 'vmx':
        mod = 'kvm_intel'

    p = subprocess.Popen(['lsmod'], stdout=subprocess.PIPE)
    out, err = p.communicate()
    if mod is not None and mod in out:
        return True

    return False


def get_preferred_libvirt_type():
    preferred = _get_preferred_libvirt_type_from_caps()

    if preferred == 'kvm' and not _kernel_mode_loaded():
        return 'qemu'

    return preferred


def _get_preferred_libvirt_type_from_caps():
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
