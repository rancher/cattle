import logging
from .utils import pool_drivers
from .config import LibvirtConfig
from dstack import utils, Config
from dstack.agent.handler import KindBasedMixin
from dstack.storage import BaseStoragePool
from dstack.type_manager import get_type_list

log = logging.getLogger('libvirt-storage')


def _get_driver(storage_pool):
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


class LibvirtStorage(KindBasedMixin, BaseStoragePool):
    def __init__(self):
        KindBasedMixin.__init__(self, kind='libvirt')
        BaseStoragePool.__init__(self)

    def _is_image_active(self, image, storage_pool):
        return _get_driver(storage_pool).is_image_active(image, storage_pool)

    def _do_image_activate(self, image, storage_pool, progress):
        return _get_driver(storage_pool).image_activate(image, storage_pool, progress)

    def _get_image_storage_pool_map_data(self, obj):
        storage_pool = obj.storage_pool
        image = obj.image
        image = _get_driver(storage_pool).get_image(image, storage_pool)
        image = self._get_image_by_id(obj.image.data.dockerImage.id)
        return {
            '+data': {
                'libvirt': image
            }
        }

#    def _get_volume_storage_pool_map_data(self, obj):
#        return {
#            'volume': {
#                'format': 'lib'
#            }
#        }

    def _is_volume_active(self, volume, storage_pool):
        return _get_driver(storage_pool).is_volume_active(volume, storage_pool)

    def _is_volume_inactive(self, volume, storage_pool):
        return _get_driver(storage_pool).is_volume_inactive(volume, storage_pool)

    def _is_volume_removed(self, volume, storage_pool):
        return _get_driver(storage_pool).is_volume_removed(volume, storage_pool)

    def _do_volume_remove(self, volume, storage_pool, progress):
        return _get_driver(storage_pool).volume_remove(volume, storage_pool, progress)
