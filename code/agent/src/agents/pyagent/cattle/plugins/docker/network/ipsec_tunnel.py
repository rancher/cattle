import logging

from .util import has_service
from cattle.utils import get_or_create_map, get_or_create_list
from cattle.agent.handler import BaseHandler

log = logging.getLogger('docker')


class IpsecTunnelSetup(BaseHandler):
    def __init__(self):
        pass

    def before_start(self, instance, host, config, start_config):
        if instance.get('agentId') is None or \
                not has_service(instance, 'ipsecTunnelService'):
            return

        try:
            id = str(host.id)
            nat = instance.data.ipsec[id]['nat']
            isakmp = instance.data.ipsec[id]['isakmp']

            ports = get_or_create_list(config, 'ports')
            binding = get_or_create_map(start_config, 'port_bindings')

            ports.append((500, 'udp'))
            ports.append((4500, 'udp'))
            binding['500/udp'] = ('0.0.0.0', isakmp)
            binding['4500/udp'] = ('0.0.0.0', nat)
        except (KeyError, AttributeError):
            pass

    def after_start(self, instance, host, id):
        pass
