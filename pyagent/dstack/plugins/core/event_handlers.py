from dstack import utils

class PingHandler:
    def execute(self, event):
        if event.name != "ping" or event.replyTo is None:
            return

        return utils.reply(event)

