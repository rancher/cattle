import logging
import os
import stat

from dstack.plugins.libvirt.drivers import LibvirtVolumeDriver
from dstack.plugins.libvirt.model import Volume
from dstack.plugins.libvirt import QemuImg
from dstack import utils

log = logging.getLogger('libvirt-qemu-img')


class QemuVolume(Volume):
    def __init__(self, file, info):
        super(Volume, self).__init__()

        self.file = file
        self.info = info

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

        temp_file = utils.temp_file_in_work_dir(base_path)

        QemuImg.create(temp_file,
                       format=self.get_format(),
                       cwd=base_path,
                       backing_file=os.path.basename(self.file))

        return QemuVolume(temp_file,
                          QemuImg.info(temp_file))

    def remove(self):
        os.remove(self.file)


class QemuImgVolumeDriver(LibvirtVolumeDriver):
    def __init__(self):
        super(QemuImgVolumeDriver, self).__init__()

    def get_supported_format(self):
        raise Exception('Unsupported operation')

    def inspect(self, storage_pool, file):
        try:
            info = QemuImg.info(file, format=self.get_supported_format())
            return QemuVolume(file, info)
        except Exception as e:
            log.debug('% is not a value %s file : exception %s', file,
                      self.get_supported_format(), e)
            return None


class Qcow2ImageDriver(QemuImgVolumeDriver):
    def __init__(self):
        super(Qcow2ImageDriver, self).__init__()

    def get_supported_format(self):
        return 'qcow2'
