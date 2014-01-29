from dstack import utils
from dstack.plugins.core.publisher import publish


class Progress(object):
    def __init__(self, req):
        self._req = req

    def update(self, msg, progress=None):
        resp = utils.reply(self._req)
        resp["transitioning"] = "yes"
        resp["transitionMessage"] = msg
        resp["transitionProgress"] = progress

        publish(resp)
