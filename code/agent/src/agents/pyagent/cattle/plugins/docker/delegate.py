import logging
import os

from urlparse import urlparse

from cattle import Config
from cattle.utils import reply
from .util import container_exec
from .compute import DockerCompute
from cattle.agent.handler import BaseHandler

log = logging.getLogger('docker')


class DockerDelegate(BaseHandler):
    def __init__(self):
        self.compute = DockerCompute()
        pass

    def events(self):
        return ['delegate.request']

    def delegate_request(self, req=None, event=None, instanceData=None, **kw):
        if instanceData.kind != 'container':
            return None

        container = self.compute.get_container_by_name(instanceData.uuid)
        if container is None:
            return

        inspect = self.compute.inspect(container)

        try:
            pid = inspect['State']['Pid']
            if not os.path.exists('/proc/{0}'.format(pid)):
                log.error('Can not call [%s], container is not running',
                          instanceData.uuid)
                return
        except KeyError:
            log.error('Can not call [%s], container is not running',
                      instanceData.uuid)
            return

        resp = container_exec(pid, event)

        if resp is None:
            return None
        else:
            return reply(req, resp)

    def before_start(self, instance, host, config):
        if instance.get('agentId') is None:
            return

        try:
            env = config['environment']
        except KeyError:
            env = {}
            config['environment'] = env

        url = Config.config_url()
        parsed = urlparse(url)

        if 'localhost' == parsed.hostname:
            env['CATTLE_CONFIG_URL_SCHEME'] = parsed.scheme
            env['CATTLE_CONFIG_URL_PATH'] = parsed.path
            env['CATTLE_CONFIG_URL_PORT'] = Config.api_proxy_listen_port()
        else:
            env['CATTLE_CONFIG_URL'] = url

    def after_start(self, instance, host, id):
        pass
