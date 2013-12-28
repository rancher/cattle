import calendar
import time
import uuid
from dstack.plugins.core.marshaller import JsonObject


def reply(event):
    if event is None or event.replyTo is None:
        return None
    else:
        return JsonObject({
            "id": str(uuid.uuid4()),
            "name": event.replyTo,
            "resourceType": event.resourceType,
            "resourceId": event.resourceId,
            "previousIds": [event.id],
            "time": calendar.timegm(time.gmtime()) * 1000,
        })


def get_data(obj, prefix=None, strip_prefix=True):
    result = {}

    if obj is None:
        return result

    data = obj.get("data")
    if data is None:
        return result

    for k, v in data.iteritems():
        if prefix is None:
            result[k] = v
            continue

        if not k.startswith(prefix):
            continue

        if strip_prefix:
            k = k[len(prefix)+1:]

        result[k] = v

    return result
