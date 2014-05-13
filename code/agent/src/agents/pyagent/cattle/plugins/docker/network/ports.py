from .util import has_service
from cattle.agent.handler import BaseHandler


class PortSetup(BaseHandler):
    def __init__(self):
        pass

    def before_start(self, instance, host, config, start_config):
        if not has_service(instance, 'portService'):
            return

        if 'ports' in config:
            del config['ports']

        start_config['publish_all_ports'] = False

    def after_start(self, instance, host, id):
        pass
