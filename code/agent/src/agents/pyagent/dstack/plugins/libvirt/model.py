
class Volume(object):
    def __init__(self):
        super(Volume, self).__init__()

    def get_format(self):
        raise Exception('Unsupported operation')

    def promote(self, dest_dir, volume, read_only=False):
        raise Exception('Unsupported operation')

    def clone(self):
        raise Exception('Unsupported operation')

    def remove(self):
        raise Exception('Unsupported operation')
