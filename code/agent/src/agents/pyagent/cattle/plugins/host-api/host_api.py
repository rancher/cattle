import os

from cattle import Config
from cattle.process_manager import background


class HostApi(object):

    def on_startup(self):
        env = dict(os.environ)
        env['HOST_API_CONFIG_FILE'] = host_api_config()

        url = 'http://{0}:{1}'.format(Config.cadvisor_ip(),
                                      Config.cadvisor_port())
        background([host_api_bin(),
                    '-cadvisor-url',  url,
                    '-logtostderr=true',
                    '-ip', Config.host_api_ip(),
                    '-port', str(Config.host_api_port())],
                   env=env)


def host_api_bin():
    return os.path.join(os.path.dirname(__file__), 'host-api')


def host_api_config():
    return os.path.join(os.path.dirname(__file__), 'host-api.conf')
