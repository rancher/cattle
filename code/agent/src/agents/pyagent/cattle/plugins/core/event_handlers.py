import os

from cattle import utils
from cattle import Config
from cattle.type_manager import types


def _should_handle(handler, event):
    name = event.name.split(';', 1)[0]
    if name not in handler.events() or event.replyTo is None:
        return False
    return True


class PingHandler:
    def __init__(self):
        pass

    def events(self):
        return ['ping']

    def execute(self, event):
        if not _should_handle(self, event):
            return

        resp = utils.reply(event)
        if Config.do_ping():
            for type in types():
                if hasattr(type, 'on_ping'):
                    type.on_ping(event, resp)

        return resp


class ConfigUpdateHandler:
    def __init__(self):
        pass

    def events(self):
        return ['config.update']

    def execute(self, event):
        if not _should_handle(self, event):
            return

        if len(event.data.items) == 0:
            return utils.reply(event)

        item_names = []

        for item in event.data.items:
            item_names.append(item.name)

        home = Config.home()

        env = dict(os.environ)
        env['CATTLE_ACCESS_KEY'] = Config.access_key()
        env['CATTLE_SECRET_KEY'] = Config.secret_key()
        env['CATTLE_CONFIG_URL'] = Config.config_url()
        env['CATTLE_HOME'] = home

        args = [Config.config_sh()] + item_names
        output = utils.get_command_output(args, cwd=home)

        return utils.reply(event, {
            'output': output
        })
