from dstack import utils

class PingHandler:
    def execute(self, event):
        if not event.name.startswith("ping") or event.replyTo is None:
            return

        return utils.reply(event)

