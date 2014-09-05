import logging

from ..util import add_to_env
from .util import has_service
from ..compute import DockerCompute
from cattle.agent.handler import BaseHandler

log = logging.getLogger('docker')


class LinkSetup(BaseHandler):
    def __init__(self):
        self.compute = DockerCompute()
        pass

    def before_start(self, instance, host, config, start_config):
        if not has_service(instance, 'linkService'):
            return

        if 'links' in start_config:
            del start_config['links']

        result = {}
        for link in instance.instanceLinks:
            name = link.linkName

            try:
                for link_port in link.data.fields.ports:
                    proto = link_port.protocol
                    ip = link_port.ipAddress
                    dst = link_port.publicPort
                    port = link_port.privatePort

                    full_port = '{0}://{1}:{2}'.format(proto, ip, dst)

                    data = {
                        'NAME': '/cattle/{0}'.format(name),
                        'PORT': full_port,
                        'PORT_{0}_{1}'.format(port, proto): full_port,
                        'PORT_{0}_{1}_ADDR'.format(port, proto): ip,
                        'PORT_{0}_{1}_PORT'.format(port, proto): dst,
                        'PORT_{0}_{1}_PROTO'.format(port, proto): proto,
                    }

                    for k, v in data.items():
                        result['{0}_{1}'.format(name, k).upper()] = v
            except AttributeError:
                pass

        if len(result) > 0:
            add_to_env(config, **result)

    def after_start(self, instance, host, id):
        pass
