import calendar
import time
import uuid


class JsonObject:
    def __init__(self, data):
        for k, v in data.items():
            if isinstance(v, dict):
                self.__dict__[k] = JsonObject(v)
            else:
                self.__dict__[k] = v

    def __getitem__(self, item):
        value = self.__dict__[item]
        if isinstance(value, JsonObject):
            return value.__dict__
        return value

    def __getattr__(self, name):
        return getattr(self.__dict__, name)


def ping_include_resources(ping):
    try:
        return ping.data.options['resources']
    except (KeyError, AttributeError):
        return False


def ping_add_resources(pong, *args):
    if 'resources' not in pong.data:
        pong.data.resources = []

    for resource in args:
        pong.data.resources.append(resource)


def events_from_methods(obj):
    ret = []
    for method in dir(obj):
        if method.startswith('_do_'):
            ret.append(method[4:].replace('_', '.'))
    return ret


def reply(event):
    if event is None or event.replyTo is None:
        return None
    else:
        return JsonObject({
            'id': str(uuid.uuid4()),
            'name': event.replyTo,
            'data': JsonObject({}),
            'resourceType': event.resourceType,
            'resourceId': event.resourceId,
            'previousIds': [event.id],
            'previousNames': [event.name],
            'time': calendar.timegm(time.gmtime()) * 1000,
        })


def get_data(obj, prefix=None, strip_prefix=True):
    result = {}

    if obj is None:
        return result

    data = obj.get('data')
    if data is None:
        return result

    for k, v in data.items():
        if prefix is None:
            result[k] = v
            continue

        if not k.startswith(prefix):
            continue

        if strip_prefix:
            k = k[len(prefix)+1:]

        result[k] = v

    return result


def memoize(function):
    memo = {}

    def wrapper(*args):
        if args in memo:
            return memo[args]
        else:
            rv = function(*args)
            memo[args] = rv
            return rv
    return wrapper
