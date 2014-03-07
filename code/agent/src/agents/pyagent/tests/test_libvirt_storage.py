from .common_fixtures import *  # NOQA

from tempfile import NamedTemporaryFile
import pytest
import shutil
from uuid import uuid4

from dstack.plugins.core.marshaller import JsonObject
from dstack import CONFIG_OVERRIDE, Config
from dstack.progress import LogProgress
from dstack import utils


if_libvirt = pytest.mark.skipif('os.environ.get("LIBVIRT_TEST") != "true"',
                                reason='LIBVIRT_TEST is not set')

from dstack.plugins.libvirt import enabled
if enabled():
    from dstack.plugins.libvirt import QemuImg
    from dstack.plugins.libvirt_qemu_volume import Qcow2ImageDriver
    from dstack.plugins.libvirt_directory_pool import DirectoryPoolDriver


QCOW_TEST_FILE = os.path.join(TEST_DIR, 'libvirt/qcow2.img')
POOL_DIR = os.path.join(TEST_DIR, 'scratch/libvirtpool')


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


@if_libvirt
def test_qcow2_inspect():
    volume = Qcow2ImageDriver().inspect(None, QCOW_TEST_FILE)

    assert volume is not None
    assert volume.get_format() == 'qcow2'

    return volume

@if_libvirt
def test_promote(pool_dir, random_qcow2):
    qcow = Qcow2ImageDriver().inspect(None, random_qcow2)

    assert qcow is not None

    qcow.promote(pool_dir, JsonObject({
        'uuid': 'test_file'
    }))

    dest_file = os.path.join(pool_dir, 'test_file.qcow2')
    assert os.path.exists(dest_file)

    os.remove(dest_file)


def _fake_volume(image_file=file):
    url = 'file://{0}'.format(file)

    ret = JsonObject({
        'uuid': str(uuid4()),
        'url': url,
        'image': _fake_image(image_file)
    })

    return ret


def _fake_image(file):
    url = 'file://{0}'.format(file)

    ret = JsonObject({
        'uuid': str(uuid4()),
        'url': url,
    })

    if file is not None:
        ret.checksum = utils.checksum(file)

    return ret


def _fake_pool(directory):
    return JsonObject({
        'kind': 'libvirt',
        'data': {
            'libvirt': {
                'poolPath': directory
            }
        }
    })


@if_libvirt
def test_download(pool_dir, random_qcow2):
    image = _fake_image(file=random_qcow2)
    pool = _fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())

    dest_file = os.path.join(pool_dir, '{0}.qcow2'.format(image.uuid))
    assert os.path.exists(dest_file)

    return image.uuid


@if_libvirt
def test_image_exists(pool_dir, random_qcow2):
    uuid = test_download(pool_dir, random_qcow2)

    image = _fake_image(None)
    pool = _fake_pool(pool_dir)
    image.uuid = uuid

    driver = DirectoryPoolDriver()
    assert driver.is_image_active(image, pool)


@if_libvirt
def test_get_image(pool_dir, random_qcow2):
    image = _fake_image(file=random_qcow2)
    pool = _fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())

    found = driver.get_image(image, pool)

    assert found is not None
    assert found.file == os.path.join(pool_dir, '{0}.qcow2'.format(image.uuid))
    assert found.get_format() == 'qcow2'


@if_libvirt
def test_volume_activate(pool_dir, random_qcow2):
    volume = _fake_volume(image_file=random_qcow2)
    image = volume.image
    pool = _fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())
    driver.volume_activate(volume, pool, LogProgress())

    found = driver.get_volume(volume, pool)

    assert found is not None
    assert found.info['backing_file'] == os.path.basename(image.file)
    assert os.path.dirname(found.file) == pool_dir
