#!/usr/bin/env python2.7

import logging
from ..type_manager import ROUTER, get_type


log = logging.getLogger("agent")


class Agent(object):
    def __init__(self):
        self._router = get_type(ROUTER)

    def execute(self, req):
        id = req.id
        try:
            log.info("Starting request %s for %s", id, req.name)
            return self._router.route(req)
        finally:
            log.info("Done request %s for %s", id, req.name)
