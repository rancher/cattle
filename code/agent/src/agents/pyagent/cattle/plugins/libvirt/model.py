
class Volume(object):
    def __init__(self):
        super(Volume, self).__init__()

    def get_disk_attributes(self):
        return {}

    def get_disk_device(self):
        return 'disk'

    def get_source_attributes(self):
        raise Exception('Unsupported operation')

    def get_target_attributes(self):
        raise Exception('Unsupported operation')

    def get_disk_type(self):
        raise Exception('Unsupported operation')

    def get_driver_name(self):
        raise Exception('Unsupported operation')

    def get_driver_type(self):
        raise Exception('Unsupported operation')

    def get_driver_attributes(self):
        return {
            'cache': 'none'
        }

    def get_format(self):
        raise Exception('Unsupported operation')

    def get_virtual_size(self):
        raise Exception('Unsupported operation')

    def get_physical_size(self):
        raise Exception('Unsupported operation')

    def promote(self, dest_dir, volume, read_only=False):
        raise Exception('Unsupported operation')

    def clone(self):
        raise Exception('Unsupported operation')

    def remove(self):
        raise Exception('Unsupported operation')

    def data(self):
        return {}
