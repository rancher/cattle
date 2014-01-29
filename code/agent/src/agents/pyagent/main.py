#!/usr/bin/env python

import sys
import os
import logging
import argparse
from dstack import plugins
from dstack import Config
from dstack.agent.event import EventClient
from dstack.type_manager import types

level = logging.INFO
if Config.debug():
    level = logging.DEBUG
logging.basicConfig(file=Config.log(), level=level)

log = logging.getLogger("agent")


def _gather_events():
    events = []
    for t in types():
        if hasattr(t, "events"):
            for e in t.events():
                events.append(e)

    return events


def _args():
    parser = argparse.ArgumentParser(add_help=True)

    parser.add_argument("--access-key", default=Config.access_key(),
                        help='Default value from DSTACK_ACCESS_KEY')
    parser.add_argument("--secret-key", default=Config.secret_key(),
                        help='Default value from DSTACK_SECRET_KEY')
    parser.add_argument("--url", default=Config.api_url(),
                        help='Default value from DSTACK_URL')
    parser.add_argument("--workers", default=Config.workers(),
                        help='Default value from DSTACK_WORKERS')
    parser.add_argument("--agent-id")

    return parser.parse_args()


def main():
    args = _args()

    plugins.load()

    auth = (args.access_key, args.secret_key)
    client = EventClient(args.url, auth=auth, workers=args.workers, agent_id=args.agent_id)
    events = _gather_events()

    log.info("Subscribing to %s", events)
    client.run(events)
    sys.exit(0)


if __name__ == '__main__':
    dist = os.path.join(os.path.dirname(__file__), "dist")
    if os.path.exists(dist):
        sys.path.insert(0, dist)

    main()
