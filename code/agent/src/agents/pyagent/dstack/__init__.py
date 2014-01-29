import os


CONFIG_OVERRIDE = {}


def _c(name, default):
    if name in CONFIG_OVERRIDE:
        return CONFIG_OVERRIDE[name]
    return os.environ.get("DSTACK_%s" % name, default)


class Config:
    def __init__(self):
        pass

    @staticmethod
    def workers():
        return int(_c("WORKERS", "25"))

    @staticmethod
    def secret_key():
        return _c("SECRET_KEY", "adminpass")

    @staticmethod
    def access_key():
        return _c("ACCESS_KEY", "admin")

    @staticmethod
    def api_url(default=None):
        return _c("URL", default)

    @staticmethod
    def api_auth():
        return Config.access_key(), Config.secret_key()

    @staticmethod
    def storage_url(default=None):
        return _c("STORAGE_URL", default)

    @staticmethod
    def config_url(default=None):
        return _c("CONFIG_URL", default)

    @staticmethod
    def is_multi_proc():
        return _c("MULTI_PROC", None) is not None

    @staticmethod
    def queue_depth():
        return int(_c("QUEUE_DEPTH", 10))

    @staticmethod
    def stop_timeout():
        return int(_c("STOP_TIMEOUT", 60))

    @staticmethod
    def log():
        return _c("LOG_FILE", "agent.log")

    @staticmethod
    def debug():
        return _c("DEBUG", "false") == "true"
