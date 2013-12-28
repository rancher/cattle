import requests
from dstack import type_manager
from dstack import log


class Publisher:
    def __init__(self, url, auth):
        self._url = url
        self._auth = auth
        self._marshaller = type_manager.get_type(type_manager.MARSHALLER)

    def publish(self, resp):
        line = self._marshaller.to_string(resp)

        log.info("Response: %s" % line)
        r = requests.post(self._url, data=line, headers={"Authentication": self._auth})
        if r.status_code != 201:
            log.error("Error [%s], Request [%s]" % (r.text, line))
