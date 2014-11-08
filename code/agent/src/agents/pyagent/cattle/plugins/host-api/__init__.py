from cattle.type_manager import register_type, LIFECYCLE

from .host_api import HostApi

register_type(LIFECYCLE, HostApi())
