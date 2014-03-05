import os

CONFIG_OVERRIDE = {}


try:
    import eventlet  # NOQA
except:
    pass


def default_value(name, default):
    if name in CONFIG_OVERRIDE:
        return CONFIG_OVERRIDE[name]
    return os.environ.get('DSTACK_%s' % name, default)


class Config:
    def __init__(self):
        pass

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
        return default_value('URL', default)

    @staticmethod
    def api_auth():
        return Config.access_key(), Config.secret_key()

    @staticmethod
    def storage_url(default=None):
        return default_value('STORAGE_URL', default)

    @staticmethod
    def config_url(default=None):
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
        return default_value('HOME', '/var/lib/dstack')

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
