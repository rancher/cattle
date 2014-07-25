from cattle.type_manager import register_type
from cattle.plugins.libvirt import enabled, LIBVIRT_COMPUTE_LISTENER
from cattle.plugins.libvirt.config import LibvirtConfig
from cattle.utils import get_command_output

from listener import ConfigDriveComputeListener

import logging

log = logging.getLogger('libvirt-config-drive')


if enabled():
    try:
        get_command_output([LibvirtConfig.genisoimage(), '--version'])
        log.info('Enabling config drive support')
        register_type(LIBVIRT_COMPUTE_LISTENER, ConfigDriveComputeListener())
    except:
        log.info('genisoimage not found, disabling config drive support')
