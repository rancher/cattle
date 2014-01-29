from dstack import utils


class PingHandler:
    def __init__(self):
        pass

    def events(self):
        return ["ping"]

    def execute(self, event):
        if not event.name.startswith("ping") or event.replyTo is None:
            return

        return utils.reply(event)
