from cattle.agent.handler import BaseHandler
from cattle.progress import Progress


class BaseStoragePool(BaseHandler):

    def __init__(self):
        super(BaseStoragePool, self).__init__()

    def _get_handler_category(self, req):
        return "storage"

    def image_activate(self, req=None, imageStoragePoolMap=None, **kw):
        image = imageStoragePoolMap.image
        storage_pool = imageStoragePoolMap.storagePool
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_image_active(image, storage_pool),
            result=lambda: self._get_response_data(imageStoragePoolMap),
            lock_obj=image,
            action=lambda: self._do_image_activate(image, storage_pool,
                                                   progress)
        )

    def volume_activate(self, req=None, volumeStoragePoolMap=None, **kw):
        volume = volumeStoragePoolMap.volume
        storage_pool = volumeStoragePoolMap.storagePool
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_volume_active(volume, storage_pool),
            result=lambda: self._get_response_data(volumeStoragePoolMap),
            lock_obj=volume,
            action=lambda: self._do_volume_activate(volume, storage_pool,
                                                    progress)
        )

    def volume_deactivate(self, req=None, volumeStoragePoolMap=None, **kw):
        volume = volumeStoragePoolMap.volume
        storage_pool = volumeStoragePoolMap.storagePool
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_volume_inactive(volume, storage_pool),
            result=lambda: self._get_response_data(volumeStoragePoolMap),
            lock_obj=volume,
            action=lambda: self._do_volume_deactivate(volume, storage_pool,
                                                      progress)
        )

    def volume_remove(self, req=None, volumeStoragePoolMap=None, **kw):
        volume = volumeStoragePoolMap.volume
        storage_pool = volumeStoragePoolMap.storagePool
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_volume_removed(volume, storage_pool),
            result=lambda: self._get_response_data(volumeStoragePoolMap),
            lock_obj=volume,
            action=lambda: self._do_volume_remove(volume, storage_pool,
                                                  progress)
        )

    def _is_image_active(self, image, storage_pool):
        raise Exception("Not implemented")

    def _do_image_activate(self, image, storage_pool, progress):
        raise Exception("Not implemented")

    def _is_volume_active(self, volume, storage_pool):
        raise Exception("Not implemented")

    def _do_volume_activate(self, volume, storage_pool, progress):
        raise Exception("Not implemented")

    def _is_volume_inactive(self, volume, storage_pool):
        raise Exception("Not implemented")

    def _do_volume_deactivate(self, volume, storage_pool, progress):
        raise Exception("Not implemented")

    def _is_volume_removed(self, volume, storage_pool):
        raise Exception("Not implemented")

    def _do_volume_remove(self, volume, storage_pool, progress):
        raise Exception("Not implemented")
