import logging
import requests
import json
import os
import sys
import uuid
import re
from Queue import Empty, Full
if os.environ.get("DSTACK_AGENT_MULTI_PROC") is None:
    from Queue import Queue
    from threading import Thread
    Worker = Thread
else:
    from multiprocessing import Queue, Process
    Worker = Process

from dstack.plugins.core.publisher import Publisher
from dstack.agent import Agent
from dstack import type_manager
from dstack import Config
from dstack import utils
from dstack.lock import FailedToLock

PSUTIL = False
if not sys.platform.startswith("linux"):
    import psutil
    PSUTIL = True

log = logging.getLogger("agent")


def _get_event_suffix(agent_id):
    parts = re.split('[a-z]+', agent_id)
    if len(parts) > 1:
        return ';agent=' + parts[1]
    else:
        return ';agent=' + agent_id


def _data(events, agent_id):
    event = {}
    if agent_id is not None:
        event['agentId'] = agent_id
        suffix = _get_event_suffix(agent_id)
        event['eventNames'] = [x + suffix for x in events]
    else:
        event['eventNames'] = events
    return json.dumps(event)


def _pid_exists(pid):
    if PSUTIL:
        return psutil.pid_exists(pid)
    else:
        return os.path.exists("/proc/%s" % pid)


def _worker(queue, ppid):
    agent = Agent()
    marshaller = type_manager.get_type(type_manager.MARSHALLER)
    publisher = type_manager.get_type(type_manager.PUBLISHER)
    while True:
        try:
            req = None
            line = queue.get(True, 5)
            log.info("Request: %s" % line)

            req = marshaller.from_string(line)
            resp = agent.execute(req)
            if resp is not None:
                publisher.publish(resp)

        except Empty:
            if not _pid_exists(ppid):
                break
        except FailedToLock as e:
            log.info("%s for %s", e, req.name)
        except Exception as e:
            error_id = str(uuid.uuid4())
            log.exception("%s : Unknown error", error_id)
            if not _pid_exists(ppid):
                break

            if req is not None:
                msg = "{0} : {1}".format(error_id, e)

                resp = utils.reply(req)
                resp["transitioning"] = "error"
                resp["transitioningInternalMessage"] = msg
                publisher.publish(resp)


class EventClient:
    def __init__(self, url, auth=None, workers=20, agent_id=None, queue_depth=Config.queue_depth()):
        if url.endswith("/schemas"):
            url = url[0:len(url)-len("/schemas")]
        self._url = url + "/subscribe"
        self._auth = auth
        self._workers = int(workers)
        self._children = []
        self._agent_id = agent_id
        self._queue = Queue(queue_depth)

        type_manager.register_type(type_manager.PUBLISHER,
                                   Publisher(url + "/publish", auth))

    def _start_children(self):
        pid = os.getpid()
        for i in range(self._workers):
            p = Worker(target=_worker, args=(self._queue, pid))
            p.daemon = True
            p.start()
            self._children.append(p)

    def run(self, events):
        ppid = os.environ.get("AGENT_PARENT_PID")
        headers = {}
        args = {
            "data": _data(events, self._agent_id),
            "stream": True,
            "headers": headers
        }

        if self._auth is not None:
            if isinstance(self._auth, basestring):
                headers["Authorization", self._auth]
            else:
                args["auth"] = self._auth

        try:
            r = requests.post(self._url, **args)
            if r.status_code != 201:
                raise Exception(r.text)

            self._start_children()

            for line in r.iter_lines(chunk_size=1):
                try:
                    if len(line) > 0:
                        self._queue.put(line, block=False)
                except Full:
                    log.info("Dropping request %s" % line)
                if ppid is not None and not _pid_exists(ppid):
                    log.info("Parent process has died, exiting")
                    break
        finally:
            for child in self._children:
                if hasattr(child, "terminate"):
                    child.terminate()
