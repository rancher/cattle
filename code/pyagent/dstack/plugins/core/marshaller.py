import json


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
    def __init__(self):
        pass

    def from_string(self, string):
        obj = json.loads(string)
        return JsonObject(obj)

    def to_string(self, obj):
        return json.dumps(dict(obj))
