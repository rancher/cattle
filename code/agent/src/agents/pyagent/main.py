#!/usr/bin/env python

import sys
import os
import logging
from logging.handlers import RotatingFileHandler
import argparse

if __name__ == '__main__':
    dist = os.path.join(os.path.dirname(__file__), "dist")
    if os.path.exists(dist):
        sys.path.insert(0, dist)

from cattle import plugins
from cattle import Config
from cattle.agent.event import EventClient
from cattle.type_manager import types, get_type_list, LIFECYCLE


log = logging.getLogger("agent")


def _setup_logger():
    format = '%(asctime)s %(levelname)s %(name)s [%(filename)s:%(lineno)s]' \
             ' %(message)s '
    level = logging.INFO
    if Config.debug():
        level = logging.DEBUG
    logging.root.setLevel(level)

    file_handler = RotatingFileHandler(Config.log(), maxBytes=2*1024*1024,
                                       backupCount=10)
    file_handler.setFormatter(logging.Formatter(format))

    std_err_handler = logging.StreamHandler(sys.stderr)
    std_err_handler.setFormatter(logging.Formatter(format))
    std_err_handler.setLevel(logging.WARN)

    logging.root.addHandler(file_handler)
    logging.root.addHandler(std_err_handler)


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
                        help='Default value from CATTLE_ACCESS_KEY')
    parser.add_argument("--secret-key", default=Config.secret_key(),
                        help='Default value from CATTLE_SECRET_KEY')
    parser.add_argument("--url", default=Config.api_url(),
                        help='Default value from CATTLE_URL')
    parser.add_argument("--workers", default=Config.workers(),
                        help='Default value from CATTLE_WORKERS')
    parser.add_argument("--agent-id")

    return parser.parse_args()


def main():
    if Config.setup_logger():
        _setup_logger()
    else:
        logging.basicConfig(level=logging.INFO)

    args = _args()

    Config.set_access_key(args.access_key)
    Config.set_secret_key(args.secret_key)
    Config.set_api_url(args.url)

    plugins.load()

    log.info('API URL %s', Config.api_url())

    client = EventClient(Config.api_url(), auth=Config.api_auth(),
                         workers=args.workers, agent_id=args.agent_id)
    events = _gather_events()

    log.info("Subscribing to %s", events)

    for startup in get_type_list(LIFECYCLE):
        startup.on_startup()

    client.run(events)
    sys.exit(0)


if __name__ == '__main__':
    main()
