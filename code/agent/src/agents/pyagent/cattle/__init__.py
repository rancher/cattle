import os
import socket

from os import path
from uuid import uuid4

from cattle.utils import memoize

CONFIG_OVERRIDE = {}


try:
    import eventlet  # NOQA
except:
    pass


def default_value(name, default):
    if name in CONFIG_OVERRIDE:
        return CONFIG_OVERRIDE[name]
    return os.environ.get('CATTLE_%s' % name, default)


_SCHEMAS = '/schemas'


def _strip_schemas(url):
    if url is None:
        return None

    if url.endswith(_SCHEMAS):
        return url[0:len(url)-len(_SCHEMAS)]

    return url


class Config:
    def __init__(self):
        pass

    @staticmethod
    @memoize
    def _get_uuid_from_file(uuid_file):
        uuid = None

        if path.exists(uuid_file):
            with open(uuid_file) as f:
                uuid = f.read().strip()
            if len(uuid) == 0:
                uuid = None

        if uuid is None:
            uuid = str(uuid4())
            with open(uuid_file, 'w') as f:
                f.write(uuid)

        return uuid

    @staticmethod
    def physical_host_uuid_file():
        def_value = '{0}/.physical_host_uuid'.format(Config.home())
        return default_value('PHYSICAL_HOST_UUID_FILE', def_value)

    @staticmethod
    def physical_host_uuid():
        return Config.get_uuid_from_file('PHYSICAL_HOST_UUID',
                                         Config.physical_host_uuid_file())

    @staticmethod
    def setup_logger():
        return default_value('LOGGER', 'true') == 'true'

    @staticmethod
    def do_ping():
        return default_value('PING_ENABLED', 'true') == 'true'

    @staticmethod
    def get_uuid_from_file(env_name, uuid_file):
        uuid = default_value(env_name, None)
        if uuid is not None:
            return uuid

        return Config._get_uuid_from_file(uuid_file)

    @staticmethod
    def hostname():
        return default_value('HOSTNAME', socket.gethostname())

    @staticmethod
    def workers():
        return int(default_value('WORKERS', '25'))

    @staticmethod
    def set_secret_key(value):
        CONFIG_OVERRIDE['SECRET_KEY'] = value

    @staticmethod
    def secret_key():
        return default_value('SECRET_KEY', 'adminpass')

    @staticmethod
    def set_access_key(value):
        CONFIG_OVERRIDE['ACCESS_KEY'] = value

    @staticmethod
    def access_key():
        return default_value('ACCESS_KEY', 'admin')

    @staticmethod
    def set_api_url(value):
        CONFIG_OVERRIDE['URL'] = value

    @staticmethod
    def api_url(default=None):
        return _strip_schemas(default_value('URL', default))

    @staticmethod
    def api_auth():
        return Config.access_key(), Config.secret_key()

    @staticmethod
    def storage_url(default=None):
        if default is None:
            default = Config.api_url()
        return default_value('STORAGE_URL', default)

    @staticmethod
    def config_url(default=None):
        if default is None:
            default = Config.api_url()
        return default_value('CONFIG_URL', default)

    @staticmethod
    def is_multi_proc():
        return Config.multi_style() == 'proc'

    @staticmethod
    def is_multi_thread():
        return Config.multi_style() == 'thread'

    @staticmethod
    def is_eventlet():
        if 'eventlet' not in globals():
            return False
        setting = default_value('AGENT_MULTI', None)

        if setting is None or setting == 'eventlet':
            return True

        return False

    @staticmethod
    def multi_style():
        return default_value('AGENT_MULTI', 'proc')

    @staticmethod
    def queue_depth():
        return int(default_value('QUEUE_DEPTH', Config.workers()))

    @staticmethod
    def stop_timeout():
        return int(default_value('STOP_TIMEOUT', 60))

    @staticmethod
    def log():
        return default_value('AGENT_LOG_FILE', 'agent.log')

    @staticmethod
    def debug():
        return default_value('DEBUG', 'false') == 'true'

    @staticmethod
    def home():
        return default_value('HOME', '/var/lib/cattle')

    @staticmethod
    def agent_ip():
        return default_value('AGENT_IP', None)

    @staticmethod
    def agent_port():
        return default_value('AGENT_PORT', None)

    @staticmethod
    def config_sh():
        return default_value('CONFIG_SCRIPT',
                             '{0}/config.sh'.format(Config.home()))

    @staticmethod
    def physical_host():
        return {
            'uuid': Config.physical_host_uuid(),
            'type': 'physicalHost',
            'kind': 'physicalHost',
            'name': Config.hostname()
        }

    @staticmethod
    def api_proxy_listen_port():
        return default_value('API_PROXY_LISTEN_PORT', 9342)

    @staticmethod
    def api_proxy_listen_host():
        return default_value('API_PROXY_LISTEN_HOST', '0.0.0.0')

    @staticmethod
    def agent_instance_cattle_home():
        return default_value('AGENT_INSTANCE_CATTLE_HOME', '/var/lib/cattle')
