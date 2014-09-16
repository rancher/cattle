from .common_fixtures import *  # NOQA

from tempfile import NamedTemporaryFile
import pytest
import shutil
from uuid import uuid4

from cattle.plugins.core.marshaller import JsonObject
from cattle.progress import LogProgress
from cattle import download


if_libvirt = pytest.mark.skipif('os.environ.get("LIBVIRT_TEST") != "true"',
                                reason='LIBVIRT_TEST is not set')

from cattle.plugins.libvirt import enabled
if enabled():
    from cattle.plugins.libvirt_directory_pool import DirectoryPoolDriver


QCOW_TEST_FILE = os.path.join(TEST_DIR, 'libvirt/qcow2.img')
QCOW_TEST_FILE_GZ = os.path.join(TEST_DIR, 'libvirt/qcow2.img.gz')
QCOW_TEST_FILE_BZ2 = os.path.join(TEST_DIR, 'libvirt/qcow2.img.bz2')
POOL_DIR = os.path.join(TEST_DIR, 'scratch/libvirtpool')


@pytest.fixture
def random_volume(pool_dir, random_qcow2):
    volume = fake_volume(image_file=random_qcow2)
    image = volume.image
    pool = fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())
    driver.volume_activate(volume, pool, LogProgress())

    return volume, driver.get_volume(volume, pool), driver, pool


@pytest.fixture(scope='module')
def pool_dir():
    if not os.path.exists(POOL_DIR):
        os.makedirs(POOL_DIR)

    return POOL_DIR


@pytest.fixture
def random_qcow2(pool_dir):
    random_file = NamedTemporaryFile(dir=pool_dir)
    random_file.close()
    shutil.copy(QCOW_TEST_FILE, random_file.name)
    return random_file.name


@pytest.fixture
def random_qcow2_bz2(pool_dir):
    random_file = NamedTemporaryFile(dir=pool_dir, suffix='.bz2')
    random_file.close()
    shutil.copy(QCOW_TEST_FILE_BZ2, random_file.name)
    return random_file.name


@pytest.fixture
def random_qcow2_gz(pool_dir):
    random_file = NamedTemporaryFile(dir=pool_dir, suffix='.gz')
    random_file.close()
    shutil.copy(QCOW_TEST_FILE_GZ, random_file.name)
    return random_file.name


def fake_volume(image_file=file):
    url = 'file://{0}'.format(file)

    ret = JsonObject({
        'id': str(uuid4()),
        'type': 'volume',
        'uuid': str(uuid4()),
        'url': url,
        'deviceNumber': 0,
        'image': fake_image(image_file)
    })

    return ret


def fake_image(file):
    url = 'file://{0}'.format(file)

    ret = JsonObject({
        'id': str(uuid4()),
        'type': 'image',
        'uuid': str(uuid4()),
        'url': url,
    })

    if file is not None:
        ret.checksum = download.checksum(file)

    return ret


def fake_pool(directory):
    return JsonObject({
        'id': str(uuid4()),
        'type': 'storagePool',
        'kind': 'libvirt',
        'data': {
            'libvirt': {
                'driver': 'directory',
                'poolPath': directory
            }
        }
    })
