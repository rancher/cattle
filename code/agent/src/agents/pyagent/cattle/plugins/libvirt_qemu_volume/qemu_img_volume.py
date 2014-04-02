import logging
import os
import stat

from cattle.plugins.libvirt.drivers import LibvirtVolumeDriver
from cattle.plugins.libvirt.model import Volume
from cattle.plugins.libvirt import QemuImg
from cattle import utils

log = logging.getLogger('libvirt-qemu-img')


class QemuVolume(Volume):
    def __init__(self, file, info, storage_pool=None, volume=None):
        super(Volume, self).__init__()

        self.volume = volume
        self.storage_pool = storage_pool
        self.file = file
        self.info = info

    def get_driver_name(self):
        return 'qemu'

    def get_driver_type(self):
        return self.get_format()

    def get_disk_type(self):
        return 'file'

    def get_source_attributes(self):
        return {
            'file': self.file
        }

    def get_target_attributes(self):
        return {
            'bus': 'virtio',
            'dev': 'vd{0}'.format(chr(self.volume.deviceNumber + 97))
        }

    def get_format(self):
        return self.info['format']

    def promote(self, dest_dir, volume, read_only=False):
        target_name = '{0}.{1}'.format(volume.uuid, self.get_format())
        dest_file = os.path.join(dest_dir, target_name)
        os.rename(self.file, dest_file)
        self.file = dest_file

        if read_only:
            os.chmod(self.file, stat.S_IREAD)

    def clone(self):
        base_path = os.path.dirname(self.file)

        temp_file = utils.temp_file(base_path)

        QemuImg.create(temp_file,
                       format=self.get_format(),
                       cwd=base_path,
                       backing_file=os.path.basename(self.file))

        return QemuVolume(temp_file,
                          QemuImg.info(temp_file,
                                       format=self.get_format()),
                          storage_pool=self.storage_pool)

    def remove(self):
        if os.path.exists(self.file):
            os.remove(self.file)

    def get_physical_size(self):
        return self.info['actual-size']

    def get_virtual_size(self):
        return self.info['virtual-size']

    def data(self):
        return self.info


class QemuImgVolumeDriver(LibvirtVolumeDriver):
    def __init__(self):
        super(QemuImgVolumeDriver, self).__init__()

    def get_supported_format(self):
        raise Exception('Unsupported operation')

    def inspect(self, storage_pool, file, volume=None):
        try:
            info = QemuImg.info(file, format=self.get_supported_format())
            return QemuVolume(file, info, volume=volume,
                              storage_pool=storage_pool)
        except Exception as e:
            log.debug('% is not a value %s file : exception %s', file,
                      self.get_supported_format(), e)
            return None


class Qcow2ImageDriver(QemuImgVolumeDriver):
    def __init__(self):
        super(Qcow2ImageDriver, self).__init__()

    def get_supported_format(self):
        return 'qcow2'
