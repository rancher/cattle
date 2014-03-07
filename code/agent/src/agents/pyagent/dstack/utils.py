from tempfile import NamedTemporaryFile
from os import path
from urllib import urlretrieve
from hashlib import md5, sha1, sha256, sha512

import calendar
import logging
import os
import time
import uuid
import subprocess

HASHES = {
    32: md5,
    40: sha1,
    64: sha256,
    128: sha512,
}


log = logging.getLogger('dstack')

_TEMP_NAME = 'work'


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


def get_data(obj, *args):
    current = obj
    for arg in args:
        child = current.get(arg)
        if child is None:
            child = {}
            current[arg] = child
        current = child

    return current


def temp_file_in_work_dir(destination):
    dst_path = path.join(destination, _TEMP_NAME)
    if not path.exists(dst_path):
        os.makedirs(dst_path)

    temp_dst = NamedTemporaryFile(dir=dst_path)
    temp_dst.close()

    return temp_dst.name


def download_file(url, destination, reporthook=None):
    temp_name = temp_file_in_work_dir(destination)

    log.info('Downloading %s to %s', url, temp_name)
    urlretrieve(url, filename=temp_name, reporthook=reporthook)

    return temp_name


def checksum(file, digest=sha1, buffer_size=2**20):
    d = digest()

    with open(file, 'rb') as input:
        while True:
            data = input.read(buffer_size)
            if not data:
                break
            d.update(data)

    return d.hexdigest()


def validate_checksum(file, checksum_value, buffer_size=2**20):
    digest = HASHES.get(len(checksum_value))

    if digest is None:
        raise Exception("Invalid checksum format")


    d = digest()

    with open(file, 'rb') as input:
        while True:
            data = input.read(buffer_size)
            if not data:
                break
            d.update(data)

    c = checksum(file, digest=digest, buffer_size=buffer_size)

    if c != checksum_value:
        raise Exception('Invalid checksum [{0}]'.format(checksum_value))


def get_command_output(*args, **kw):
    try:
        return subprocess.check_output(*args, **kw)
    except subprocess.CalledProcessError as e:
        log.exception('Failed to call %s %s, exit [%s], output :\n%s', args, kw,
                      e.returncode, e.output)
        raise e
