from hashlib import md5
from os import path

import glob
import os
import logging
import time

from cattle.download import download_file
from cattle.plugins.libvirt import LIBVIRT_KIND, volume_drivers
from cattle.plugins.libvirt.config import LibvirtConfig
from cattle.plugins.libvirt.drivers import LibvirtStoragePoolDriver

log = logging.getLogger('libvirt-directory-pool')

POOL_DRIVER_NAME = 'directory'


class DirectoryPoolDriver(LibvirtStoragePoolDriver):

    def __init__(self):
        super(DirectoryPoolDriver, self).__init__()

    @staticmethod
    def _get_path(storage_pool):
        return storage_pool.data.libvirt.poolPath

    def driver_name(self):
        return POOL_DRIVER_NAME

    def discover(self, compute):
        ret = []

        if 'directory' in LibvirtConfig.pool_drivers():
            for p in LibvirtConfig.pool_directories():
                if not path.exists(p):
                    os.makedirs(p)

                uuid = '{0}-{1}'.format(compute['uuid'], md5(p).hexdigest())
                pool = {
                    'type': 'storagePool',
                    'kind': LIBVIRT_KIND,
                    'name': '{0} Storage Pool {1}'.format(compute['name'], p),
                    'hostUuid': compute['uuid'],
                    'uuid': uuid,
                    'data': {
                        'libvirt': {
                            'poolPath': p
                        }
                    }
                }

                ret.append(pool)

        return ret

    def _find_file_by_uuid(self, storage_pool, uuid):
        pool_path = self._get_path(storage_pool)

        ret = glob.glob(os.path.join(pool_path, '{0}.*'.format(uuid)))

        if len(ret) > 0:
            return ret[0]
        else:
            return None

    def get_image(self, image, storage_pool):
        return self._get_object(image, storage_pool)

    def get_volume(self, volume, storage_pool):
        return self._get_object(volume, storage_pool)

    def _get_object(self, image, storage_pool):
        if image is None:
            return None

        file = self._find_file_by_uuid(storage_pool, image.uuid)

        if file is None:
            return None

        for driver in volume_drivers():
            image = driver.inspect(storage_pool, file, volume=image)
            if image is not None:
                break

        return image

    def _is_active(self, obj, storage_pool):
        return self._find_file_by_uuid(storage_pool, obj.uuid) is not None

    def is_image_active(self, image, storage_pool):
        return self._is_active(image, storage_pool)

    def is_volume_inactive(self, image, storage_pool):
        return True

    def is_volume_active(self, volume, storage_pool):
        return self._is_active(volume, storage_pool)

    def image_activate(self, image, storage_pool, progress):
        state = {
            'percent': 0,
            'time': time.time(),
            'counter': 0
        }
        check_every = 100
        counter = 0

        def report(*args):
            state['counter'] += 1
            count, size, total = args

            if total <= 0:
                return

            percent = (((count*size*100)/total)/3)*3
            if state['percent'] != percent or \
                ((counter % check_every) == 0 and
                 time.time() - state['time'] >= 2):
                state['percent'] = percent
                state['time'] = time.time()
                state['total'] = total
                state['current'] = count*size

                log.info('Progress %s %s', image.url, state)
                progress.update(None, progress=percent, data=state)

        pool_path = self._get_path(storage_pool)
        downloaded = None
        try:
            downloaded = download_file(image.url,
                                       pool_path,
                                       reporthook=report,
                                       checksum=image.checksum)

            volume = None
            for driver in volume_drivers():
                volume = driver.inspect(storage_pool, downloaded)
                if volume is not None:
                    break

            if volume is None:
                raise Exception('Unsupported volume format')

            volume.promote(pool_path, image, read_only=True)

        finally:
            if downloaded is not None and path.exists(downloaded):
                os.remove(downloaded)

    def volume_activate(self, volume, storage_pool, progress):
        image = self.get_image(volume.image, storage_pool)

        if image is None:
            raise Exception('Only volumes from images are supported right now')

        cloned = image.clone()

        pool_path = self._get_path(storage_pool)

        cloned.promote(pool_path, volume)

    def is_volume_removed(self, volume, storage_pool):
        return self._get_object(volume, storage_pool) is None

    def volume_remove(self, volume, storage_pool, progress):
        volume = self._get_object(volume, storage_pool)
        volume.remove()
