import urlparse
import time
import logging
import os
import socket

from cattle import Config
from cattle.concurrency import spawn
from cattle.utils import get_command_output, get_url_port

log = logging.getLogger('api-proxy')


class ApiProxy(object):
    def __init__(self):
        self.pid = None

    def on_startup(self):
        url = Config.config_url()

        if 'localhost' not in url:
            return

        parsed = urlparse.urlparse(url)
        port = get_url_port(url)

        spawn(target=proxy, args=(Config.api_proxy_listen_host(),
                                  Config.api_proxy_listen_port(),
                                  parsed.hostname,
                                  port))


def proxy(from_host, from_port, to_host, to_port):
    while True:
        to_host_ip = socket.gethostbyname(to_host)
        log.info('Proxying %s:%s -> %s:%s', from_host, from_port, to_host_ip,
                 to_port)
        listen = 'TCP4-LISTEN:{0},fork,bind={1},reuseaddr'.format(from_port,
                                                                  from_host)
        to = 'TCP:{0}:{1}'.format(to_host_ip, to_port)
        try:
            get_command_output([socat_bin(), listen, to])
        except:
            log.exception('Failed to run socat')
            pass

        time.sleep(5)


def socat_bin():
    return os.path.join(os.path.dirname(__file__), 'socat_wrapper.sh')