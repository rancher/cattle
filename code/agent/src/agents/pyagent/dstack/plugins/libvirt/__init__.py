import logging

log = logging.getLogger('libvirt')

_ENABLED = False
try:
    import libvirt

    try:
        conn = libvirt.open()
        conn.close()
    except:
        log.info('Failed to get connection to libvirt')

    _ENABLED = True
except:
    pass

LIBVIRT_KIND = 'libvirt'

if _ENABLED:
    from dstack.type_manager import register_type
    from dstack.type_manager import COMPUTE_DRIVER, STORAGE_DRIVER

    from .storage import LibvirtStorage
    from .compute import LibvirtCompute
    from .utils import *  # NOQA

    register_type(COMPUTE_DRIVER, LibvirtCompute())
    register_type(STORAGE_DRIVER, LibvirtStorage())
else:
    log.info('Disabling libvirt, libvirt-python not found')

from qemu_img import QemuImg  # NOQA


def enabled():
    return _ENABLED
