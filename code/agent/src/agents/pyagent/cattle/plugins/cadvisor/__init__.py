from cattle.type_manager import register_type, LIFECYCLE
from .cadvisor import Cadvisor

register_type(LIFECYCLE, Cadvisor())
