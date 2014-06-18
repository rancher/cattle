import urlparse
import logging
import os
import socket

from cattle import Config
from cattle.utils import get_url_port
from cattle.process_manager import background

log = logging.getLogger('api-proxy')


class ApiProxy(object):
    def __init__(self):
        self.pid = None

    def on_startup(self):
        url = Config.config_url()

        if 'localhost' not in url:
            return

        parsed = urlparse.urlparse(url)

        from_host = Config.api_proxy_listen_host()
        from_port = Config.api_proxy_listen_port()
        to_host_ip = socket.gethostbyname(parsed.hostname)
        to_port = get_url_port(url)

        log.info('Proxying %s:%s -> %s:%s', from_host, from_port, to_host_ip,
                 to_port)
        listen = 'TCP4-LISTEN:{0},fork,bind={1},reuseaddr'.format(from_port,
                                                                  from_host)
        to = 'TCP:{0}:{1}'.format(to_host_ip, to_port)

        background([socat_bin(), listen, to])


def socat_bin():
    return os.path.join(os.path.dirname(__file__), 'socat')