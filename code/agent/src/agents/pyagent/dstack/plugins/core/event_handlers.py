import os
import subprocess

from dstack import utils
from dstack import Config
from dstack.type_manager import types


def _should_handle(handler, event):
    name = event.name.split(';', 1)[0]
    if not name in handler.events() or event.replyTo is None:
        return False
    return True


class PingHandler:
    def __init__(self):
        pass

    def events(self):
        return ["ping"]

    def execute(self, event):
        if not _should_handle(self, event):
            return

        resp = utils.reply(event)
        for type in types():
            if hasattr(type, 'on_ping'):
                type.on_ping(event, resp)

        return resp


class ConfigUpdateHandler:
    def __init__(self):
        pass

    def events(self):
        return []
        #return ["config.update"]

    def execute(self, event):
        if not _should_handle(self, event):
            return

        if len(event.data.items) == 0:
            return utils.reply(event)

        env = dict(os.environ)
        env["DSTACK_ACCESS_KEY"] = Config.access_key()
        env["DSTACK_SECRET_KEY"] = Config.secret_key()

        args = [Config.config_sh(), "--url",
                Config.config_url(event.data.configUrl)]
        for item in event.data.items:
            args.append(item)

        subprocess.check_call(*args, env=env)

        return utils.reply(event)
