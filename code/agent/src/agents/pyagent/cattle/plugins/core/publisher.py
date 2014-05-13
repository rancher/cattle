import logging
import requests
import time
from cattle import type_manager
from cattle.utils import log_request


log = logging.getLogger("agent")


class Publisher:
    def __init__(self, url, auth):
        self._url = url
        self._auth = auth
        self._marshaller = type_manager.get_type(type_manager.MARSHALLER)
        self._session = requests.Session()

    def publish(self, resp):
        line = self._marshaller.to_string(resp)

        start = time.time()
        try:
            r = self._session.post(self._url, data=line, auth=self._auth,
                                   timeout=5)
            if r.status_code != 201:
                log.error("Error [%s], Request [%s]", r.text, line)
        finally:
            log_request(resp, log, 'Response: %s [%s] seconds', line,
                        time.time() - start)

    @property
    def url(self):
        return self._url

    @property
    def auth(self):
        return self._auth
