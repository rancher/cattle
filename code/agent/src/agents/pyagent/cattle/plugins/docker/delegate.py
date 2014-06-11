import logging
import os

from urlparse import urlparse

from cattle import Config
from cattle.utils import reply
from .util import add_to_env
from .compute import DockerCompute
from cattle.agent.handler import BaseHandler
from cattle.progress import Progress
from cattle.type_manager import get_type, MARSHALLER

import requests

log = logging.getLogger('docker')

_SESSION = requests.Session()


def container_exec(ip, token, event):
    marshaller = get_type(MARSHALLER)
    data = marshaller.to_string(event)
    url = 'http://{0}:8080/events?token={1}'.format(ip, token)

    r = _SESSION.post(url, data=data, headers={
        'Content-Type': 'application/json'
    })

    if r.status_code != 200:
        return r.status_code, r.text, None

    result = r.json()

    data = result.get('data')
    if data is not None:
        data = marshaller.from_string(data)

    return result.get('exitCode'), result.get('output'), data


class DockerDelegate(BaseHandler):
    def __init__(self):
        self.compute = DockerCompute()
        pass

    def events(self):
        return ['delegate.request']

    def delegate_request(self, req=None, event=None, instanceData=None, **kw):
        if instanceData.kind != 'container' or\
                        instanceData.get('token') is None:
            return

        container = self.compute.get_container_by_name(instanceData.uuid)
        if container is None:
            return

        inspect = self.compute.inspect(container)

        try:
            ip = inspect['NetworkSettings']['IPAddress']
            running = inspect['State']['Running']
            if not running:
                log.error('Can not call [%s], container is not running',
                          instanceData.uuid)
                return
        except KeyError:
            log.error('Can not call [%s], container is not running',
                      instanceData.uuid)
            return

        progress = Progress(event, parent=req)
        exit_code, output, data = container_exec(ip, instanceData.token, event)

        if exit_code == 0:
            return reply(event, data, parent=req)
        else:
            progress.update('Update failed', data={
                'exitCode': exit_code,
                'output': output
            })

    def before_start(self, instance, host, config, start_config):
        if instance.get('agentId') is None:
            return

        url = Config.config_url()
        parsed = urlparse(url)

        if 'localhost' == parsed.hostname:
            add_to_env(config,
                       CATTLE_CONFIG_URL_SCHEME=parsed.scheme,
                       CATTLE_CONFIG_URL_PATH=parsed.path,
                       CATTLE_CONFIG_URL_PORT=Config.api_proxy_listen_port())
        else:
            add_to_env(config, CATTLE_CONFIG_URL=url)

    def after_start(self, instance, host, id):
        pass
