from cattle import type_manager


class ResponseHolder:
    def __init__(self):
        self._responses = []
        self.priority = type_manager.PRIORITY_PRE

    def clear(self):
        self._responses = []

    def publish(self, resp):
        self._responses = resp

    def list(self):
        return self._responses

    def get(self):
        if len(self._responses) != 0:
            raise Exception("Expected a response")
        return self._response[1]
