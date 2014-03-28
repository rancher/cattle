import logging

from cattle import utils
from cattle.plugins.core.publisher import publish

log = logging.getLogger('progress')


class EventProgress(object):
    def __init__(self, req):
        self._req = req

    def update(self, msg, progress=None):
        resp = utils.reply(self._req)
        resp["transitioning"] = "yes"
        resp["transitionMessage"] = msg
        resp["transitionProgress"] = progress

        publish(resp)

Progress = EventProgress


class LogProgress(object):
    def __init__(self):
        pass

    def update(self, msg, progress=None):
        log.info('Progress %s %s', msg, progress)
