#!/usr/bin/env python2

import common_fixtures
from dstack.agent.event import EventClient


def run_agent_connect():
    client = EventClient("http://localhost:8080/v1", workers=1)
    client.run(["*agent*"])


if __name__ == "__main__":
    run_agent_connect()
