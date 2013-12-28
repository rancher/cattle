import requests
import json
import sys
import os
import psutil
from Queue import Empty, Full
from multiprocessing import Queue, Process

from dstack.plugins.core.publisher import Publisher
from dstack.agent import Agent
from dstack import log
from dstack import type_manager


def _data(events):
    event = {"eventNames": events}
    return json.dumps(event)


def _worker(queue, ppid):
    agent = Agent()
    marshaller = type_manager.get_type(type_manager.MARSHALLER)
    publisher = type_manager.get_type(type_manager.PUBLISHER)
    while True:
        try:
            line = queue.get(True, 5)
            log.info("Request: %s" % line)

            req = marshaller.from_string(line)
            resp = agent.execute(req)
            if resp is not None:
                publisher.publish(resp)

        except Empty:
            if not psutil.pid_exists(ppid):
                break
        except Exception as e:
            log.exception("Unknown error")
            if not psutil.pid_exists(ppid):
                break


class EventClient:
    def __init__(self, url, auth=None, workers=20):
        self._url = url + "/subscribe"
        self._auth = auth
        self._workers = workers
        self._children = []
        self._queue = Queue(1)

        type_manager.register_type(type_manager.PUBLISHER, Publisher(url + "/publish", auth))


    def _start_children(self):
        pid = os.getpid()
        for i in range(self._workers):
            p = Process(target=_worker, args=(self._queue, pid))
            p.start()
            self._children.append(p)

    def run(self, events):
        headers = {}
        if self._auth is not None:
            headers["Authentication", self._auth]

        try:
            r = requests.post(self._url, data=_data(events), stream=True, headers=headers)
            self._start_children()
            for line in r.iter_lines(chunk_size=1):
                try:
                    self._queue.put(line, block=False)
                except Full:
                    log.info("Dropping request %s" % line)
        finally:
            for child in self._children:
                child.terminate()

