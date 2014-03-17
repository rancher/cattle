import logging
import math
from .utils import pool_drivers
from dstack.agent.handler import KindBasedMixin
from dstack.storage import BaseStoragePool

log = logging.getLogger('libvirt-storage')


def get_pool_driver(storage_pool):
    try:
        driver_name = storage_pool.data.libvirt.driver
        for driver in pool_drivers():
            if driver.driver_name() == driver_name:
                return driver

        raise Exception('Failed to find libvirt pool driver for {}'
                        .format(driver_name))
    except AttributeError:
        raise Exception('Failed to find data.libvirt.driver in {}'
                        .format(storage_pool))


def _to_megabytes(bytes):
    return int(math.ceil(bytes/(1024**2)))


class LibvirtStorage(KindBasedMixin, BaseStoragePool):
    def __init__(self):
        KindBasedMixin.__init__(self, kind='libvirt')
        BaseStoragePool.__init__(self)

    def _is_image_active(self, image, storage_pool):
        return get_pool_driver(storage_pool).is_image_active(image, storage_pool)

    def _do_image_activate(self, image, storage_pool, progress):
        return get_pool_driver(storage_pool).image_activate(image, storage_pool,
                                                        progress)

    @staticmethod
    def _get_image_storage_pool_map_data(image_storage_pool_map):
        image = image_storage_pool_map.image
        storage_pool = image_storage_pool_map.storagePool
        image_obj = get_pool_driver(storage_pool).get_image(image, storage_pool)
        return {
            'virtualSizeMb': _to_megabytes(image_obj.get_virtual_size()),
            'physicalSizeMb': _to_megabytes(image_obj.get_physical_size()),
            'format': image_obj.get_format(),
            '+data': {
                'libvirt': image_obj.data()
            }
        }

    @staticmethod
    def _get_volume_storage_pool_map_data(volume_storage_pool_map):
        volume = volume_storage_pool_map.volume
        storage_pool = volume_storage_pool_map.storagePool
        volume_obj = get_pool_driver(storage_pool).get_volume(volume, storage_pool)
        return {
            'virtualSizeMb': _to_megabytes(volume_obj.get_virtual_size()),
            'format': volume_obj.get_format(),
            '+data': {
                'libvirt': volume_obj.data()
            }
        }

    def _is_volume_active(self, volume, storage_pool):
        return get_pool_driver(storage_pool).is_volume_active(volume, storage_pool)

    def _do_volume_activate(self, volume, storage_pool, progress):
        return get_pool_driver(storage_pool).volume_activate(volume, storage_pool,
                                                         progress)

    def _is_volume_inactive(self, volume, storage_pool):
        return get_pool_driver(storage_pool).is_volume_inactive(volume, storage_pool)

    def _is_volume_removed(self, volume, storage_pool):
        return get_pool_driver(storage_pool).is_volume_removed(volume, storage_pool)

    def _do_volume_remove(self, volume, storage_pool, progress):
        return get_pool_driver(storage_pool).volume_remove(volume, storage_pool,
                                                       progress)
