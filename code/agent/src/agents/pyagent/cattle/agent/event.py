import json
import logging
import os
import re
import requests
import sys
import time
import uuid

from cattle import Config
from cattle import type_manager
from cattle import utils
from cattle.agent import Agent
from cattle.lock import FailedToLock
from cattle.plugins.core.publisher import Publisher
from cattle.concurrency import Queue, Full, Empty, run, spawn


PS_UTIL = False
if not sys.platform.startswith("linux"):
    import psutil
    PS_UTIL = True

log = logging.getLogger("agent")
_STAMP_TS = None


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


def _check_ts():
    stamp_file = Config.stamp()
    if not os.path.exists(stamp_file):
        return True

    ts = os.path.getmtime(stamp_file)
    global _STAMP_TS

    if _STAMP_TS is None:
        _STAMP_TS = ts

    return _STAMP_TS == ts


def _should_run(pid):
    if not _check_ts():
        return False

    if PS_UTIL:
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

            id = req.id
            start = time.time()
            try:
                log.info("Starting request %s for %s", id, req.name)
                resp = agent.execute(req)
                if resp is not None:
                    publisher.publish(resp)
            finally:
                duration = time.time() - start
                log.info("Done request %s for %s [%s] seconds", id, req.name,
                         duration)
        except Empty:
            if not _should_run(ppid):
                break
        except FailedToLock as e:
            log.info("%s for %s", e, req.name)
        except Exception as e:
            error_id = str(uuid.uuid4())
            log.exception("%s : Unknown error", error_id)
            if not _should_run(ppid):
                break

            if req is not None:
                msg = "{0} : {1}".format(error_id, e)

                resp = utils.reply(req)
                if resp is not None:
                    resp["transitioning"] = "error"
                    resp["transitioningInternalMessage"] = msg
                    publisher.publish(resp)


class EventClient:
    def __init__(self, url, auth=None, workers=20, agent_id=None,
                 queue_depth=Config.queue_depth()):
        if url.endswith("/schemas"):
            url = url[0:len(url)-len("/schemas")]
        self._url = url + "/subscribe"
        self._auth = auth
        self._workers = int(workers)
        self._children = []
        self._agent_id = agent_id
        self._queue = Queue(queue_depth)
        self._ping_queue = Queue(queue_depth)

        type_manager.register_type(type_manager.PUBLISHER,
                                   Publisher(url + "/publish", auth))

    def _start_children(self):
        pid = os.getpid()
        for i in range(self._workers):
            p = spawn(target=_worker, args=(self._queue, pid))
            self._children.append(p)

        p = spawn(target=_worker, args=(self._ping_queue, pid))
        self._children.append(p)

    def run(self, events):
        _check_ts()
        run(self._run, events)

    def _run(self, events):
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
                        # TODO Need a better approach here
                        if '"ping' in line:
                            self._ping_queue.put(line, block=False)
                        else:
                            self._queue.put(line, block=False)
                except Full:
                    log.info("Dropping request %s" % line)
                if ppid is not None and not _should_run(ppid):
                    log.info("Parent process has died or stamp changed,"
                             " exiting")
                    break
        finally:
            for child in self._children:
                if hasattr(child, "terminate"):
                    child.terminate()
