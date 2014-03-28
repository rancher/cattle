#!/usr/bin/env python2.7

import logging
from ..type_manager import ROUTER, get_type


log = logging.getLogger("agent")


class Agent(object):
    def __init__(self):
        self._router = get_type(ROUTER)

    def execute(self, req):
        return self._router.route(req)
