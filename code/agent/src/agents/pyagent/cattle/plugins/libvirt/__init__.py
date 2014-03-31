import logging

log = logging.getLogger('libvirt')

_ENABLED = False
try:
    import libvirt

    try:
        conn = libvirt.open('qemu:///system')
        conn.close()
        _ENABLED = True
    except:
        log.info('Failed to get connection to libvirt')

except:
    log.info('Failed to find libvirt python')
    pass

LIBVIRT_KIND = 'libvirt'

if _ENABLED:
    from cattle.type_manager import register_type
    from cattle.type_manager import COMPUTE_DRIVER, STORAGE_DRIVER

    from .storage import LibvirtStorage
    from .compute import LibvirtCompute
    from .utils import *  # NOQA

    register_type(COMPUTE_DRIVER, LibvirtCompute())
    register_type(STORAGE_DRIVER, LibvirtStorage())
else:
    log.info('Disabling libvirt')

from qemu_img import QemuImg  # NOQA


def enabled():
    return _ENABLED
