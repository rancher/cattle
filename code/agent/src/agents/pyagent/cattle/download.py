from urllib import urlretrieve
from hashlib import md5, sha1, sha256, sha512

import bz2
import gzip
import logging
import os

from concurrency import blocking
from utils import temp_file_in_work_dir


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

CHUNK_SIZE = 8192

log = logging.getLogger('cattle')


def download_file(url, destination, reporthook=None, decompression=True,
                  checksum=None):
    return blocking(_download_file, url, destination, reporthook=reporthook,
                    decompression=decompression, checksum=checksum)


def _download_file(url, destination, reporthook=None, decompression=True,
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
                while True:
                    chunk = input.read(CHUNK_SIZE)
                    if chunk:
                        output.write(chunk)
                    else:
                        break

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
