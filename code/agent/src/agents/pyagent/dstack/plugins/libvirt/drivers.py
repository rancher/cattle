class LibvirtStoragePoolDriver(object):
    def __init__(self):
        pass

    def driver_name(self):
        raise Exception("Unsupported operation")

    def discover(self, host):
        raise Exception("Unsupported operation")

    def get_image(self, image, storage_pool):
        raise Exception("Unsupported operation")

    def is_image_active(self, image, storage_pool):
        raise Exception("Unsupported operation")

    def image_activate(self, image, storage_pool, progress):
        raise Exception("Unsupported operation")

    def get_volume(self, image, storage_pool):
        raise Exception("Unsupported operation")

    def is_volume_active(self, volume, storage_pool):
        raise Exception("Unsupported operation")

    def is_volume_inactive(self, volume, storage_pool):
        raise Exception("Unsupported operation")

    def is_volume_removed(self, volume, storage_pool):
        raise Exception("Unsupported operation")

    def volume_remove(self, volume, storage_pool, progress):
        raise Exception("Unsupported operation")


class LibvirtVolumeDriver(object):
    def __init__(self):
        pass

    def inspect(self, storage_pool, file):
        raise Exception("Unsupported operation")
