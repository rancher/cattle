from tempfile import NamedTemporaryFile
from os import path
from urllib import urlretrieve
from hashlib import md5, sha1, sha256, sha512

import binascii
import bz2
import calendar
import gzip
import logging
import os
import re
import subprocess
import time
import uuid
from subprocess import PIPE, Popen, CalledProcessError

HASHES = {
    32: md5,
    40: sha1,
    64: sha256,
    128: sha512,
}

DECOMPRESS = {
    '.gz': gzip.open,
    '.bz2': bz2.BZ2File
}

log = logging.getLogger('cattle')

_TEMP_NAME = 'work'
_TEMP_PREFIX = 'cattle-temp-'


def _to_json_object(v):
    if isinstance(v, dict):
        return JsonObject(v)
    elif isinstance(v, list):
        ret = []
        for i in v:
            ret.append(_to_json_object(i))
        return ret
    else:
        return v


class JsonObject:
    def __init__(self, data):
        for k, v in data.items():
            self.__dict__[k] = _to_json_object(v)

    def __getitem__(self, item):
        value = self.__dict__[item]
        if isinstance(value, JsonObject):
            return value.__dict__
        return value

    def __getattr__(self, name):
        return getattr(self.__dict__, name)

    @staticmethod
    def unwrap(json_object):
        if isinstance(json_object, list):
            ret = []
            for i in json_object:
                ret.append(JsonObject.unwrap(i))
            return ret

        if isinstance(json_object, dict):
            ret = {}
            for k, v in json_object.items():
                ret[k] = JsonObject.unwrap(v)
            return ret

        if isinstance(json_object, JsonObject):
            ret = {}
            for k, v in json_object.__dict__.items():
                ret[k] = JsonObject.unwrap(v)
            return ret

        return json_object


def ping_include_resources(ping):
    try:
        return ping.data.options['resources']
    except (KeyError, AttributeError):
        return False


def ping_include_instances(ping):
    try:
        return ping.data.options['instances']
    except (KeyError, AttributeError):
        return False


def ping_add_resources(pong, *args):
    if 'resources' not in pong.data:
        pong.data.resources = []

    for resource in args:
        pong.data.resources.append(resource)


def ping_set_option(pong, key, value):
    if 'options' not in pong.data:
        pong.data.options = {}

    pong.data.options[key] = value


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


def get_map_value(obj, *args):
    current = obj
    for arg in args:
        child = current.get(arg)
        if child is None:
            child = {}
            current[arg] = child
        current = child

    return current


def temp_file(destination):
    temp_dst = NamedTemporaryFile(prefix=_TEMP_PREFIX, dir=destination)
    temp_dst.close()

    return temp_dst.name


def temp_file_in_work_dir(destination):
    dst_path = path.join(destination, _TEMP_NAME)
    if not path.exists(dst_path):
        os.makedirs(dst_path)

    return temp_file(dst_path)


def download_file(url, destination, reporthook=None, decompression=True,
                  checksum=None):
    temp_name = temp_file_in_work_dir(destination)

    log.info('Downloading %s to %s', url, temp_name)
    urlretrieve(url, filename=temp_name, reporthook=reporthook)

    if checksum is not None:
        validate_checksum(temp_name, checksum)

    if decompression:
        decompress(temp_name, url)

    return temp_name


def decompress(file_name, name=None):
    if name is None:
        name = file_name

    temp_name = '{0}.extract-temp'.format(file_name)
    for ext, archive_open in DECOMPRESS.items():
        if name.endswith(ext):
            with open(temp_name, 'wb') as output:
                input = archive_open(file_name)
                for line in input:
                    output.write(line)
                input.close()

            os.rename(temp_name, file_name)
            return True

    return False


def checksum(file, digest=sha1, buffer_size=2**20):
    d = digest()

    with open(file, 'rb') as input:
        while True:
            data = input.read(buffer_size)
            if not data:
                break
            d.update(data)

    return d.hexdigest()


def validate_checksum(file_name, checksum_value, buffer_size=2**20):
    digest = HASHES.get(len(checksum_value))

    if digest is None:
        raise Exception("Invalid checksum format")

    d = digest()

    with open(file_name, 'rb') as input:
        while True:
            data = input.read(buffer_size)
            if not data:
                break
            d.update(data)

    c = checksum(file_name, digest=digest, buffer_size=buffer_size)

    if c != checksum_value:
        raise Exception('Invalid checksum [{0}]'.format(checksum_value))


def get_command_output(*args, **kw):
    try:
        return _check_output(*args, **kw)
    except subprocess.CalledProcessError as e:
        log.exception('Failed to call %s %s, exit [%s], output :\n%s', args,
                      kw, e.returncode, e.output)
        raise e


def _check_output(*popenargs, **kwargs):
    if 'check_output' in dir(subprocess):
        return subprocess.check_output(*popenargs, **kwargs)

    # Copyright (c) 2003-2005 by Peter Astrand <astrand@lysator.liu.se>
    #
    # Licensed to PSF under a Contributor Agreement.
    # See http://www.python.org/2.4/license for licensing details.
    if 'stdout' in kwargs:
        raise ValueError('stdout argument not allowed, it will be overridden.')
    process = Popen(stdout=PIPE, *popenargs, **kwargs)
    output, unused_err = process.communicate()
    retcode = process.poll()
    if retcode:
        cmd = kwargs.get("args")
        if cmd is None:
            cmd = popenargs[0]
        raise CalledProcessError(retcode, cmd, output=output)
    return output


def random_string(length=64):
    return binascii.hexlify(os.urandom(length/2))


def is_uuid(str):
    if str is None:
        return False
    return re.match(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}'
                    r'-[0-9a-f]{4}-[0-9a-f]{12}', str) is not None
