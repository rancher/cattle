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
        ret.checksum = utils.checksum(file)

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


@if_libvirt
def test_download(pool_dir, random_qcow2):
    image = fake_image(file=random_qcow2)
    pool = fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())

    dest_file = os.path.join(pool_dir, '{0}.qcow2'.format(image.uuid))
    assert os.path.exists(dest_file)

    return image.uuid


@if_libvirt
def test_image_exists(pool_dir, random_qcow2):
    uuid = test_download(pool_dir, random_qcow2)

    image = fake_image(None)
    pool = fake_pool(pool_dir)
    image.uuid = uuid

    driver = DirectoryPoolDriver()
    assert driver.is_image_active(image, pool)


@if_libvirt
def test_get_image(pool_dir, random_qcow2):
    image = fake_image(file=random_qcow2)
    pool = fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())

    found = driver.get_image(image, pool)

    assert found is not None
    assert found.file == os.path.join(pool_dir, '{0}.qcow2'.format(image.uuid))
    assert found.get_format() == 'qcow2'
    assert not os.access(found.file, os.W_OK)


@if_libvirt
def test_volume_activate(pool_dir, random_qcow2):
    volume = fake_volume(image_file=random_qcow2)
    image = volume.image
    pool = fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())
    driver.volume_activate(volume, pool, LogProgress())

    found = driver.get_volume(volume, pool)
    found_image = driver.get_image(image, pool)

    assert found is not None
    assert found.info['backing-filename'] == os.path.basename(found_image.file)
    assert found.info['virtual-size'] == found_image.info['virtual-size']
    assert os.path.dirname(found.file) == pool_dir


@if_libvirt
def test_volume_remove(random_volume, pool_dir, random_qcow2):
    volume, volume_obj, driver, pool = random_volume
    image_obj = driver.get_image(volume.image, pool)

    assert os.path.exists(volume_obj.file)
    assert os.path.exists(image_obj.file)
    assert not driver.is_volume_removed(volume, pool)

    driver.volume_remove(volume, pool, LogProgress())

    assert not os.path.exists(volume_obj.file)
    assert os.path.exists(image_obj.file)
    assert driver.is_volume_removed(volume, pool)

@if_libvirt
def test_volume_deactivate(random_volume, pool_dir, random_qcow2):
    volume, volume_obj, driver, pool = random_volume
    image_obj = driver.get_image(volume.image, pool)

    # There is not deactivate in a directory pool
    assert driver.is_volume_inactive(volume, pool)
