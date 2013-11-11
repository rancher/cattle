import json
import collections


class JsonObject:
    def __init__(self, data):
        for k, v in data.iteritems():
            if isinstance(v, dict):
                self.__dict__[k] = JsonObject(v)
            else:
                self.__dict__[k] = v
    def __getattr__(self, name):
        return getattr(self.__dict__, name)


class Marshaller:
    def from_string(self, string):
        obj = json.loads(string)
        return JsonObject(obj)

    def to_string(self, string):
        return repr(string)
