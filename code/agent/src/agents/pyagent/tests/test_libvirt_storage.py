from .common_fixtures import *  # NOQA
from .common_libvirt_fixtures import *  # NOQA

import stat

if enabled():
    from cattle.plugins.libvirt_qemu_volume import Qcow2ImageDriver


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

    _check_file_mode(found.file, '400')


def _check_file_mode(file_name, str_mode):
    assert oct(os.stat(file_name)[stat.ST_MODE])[-3:] == str_mode


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
    driver.get_image(volume.image, pool)

    # There is not deactivate in a directory pool
    assert driver.is_volume_inactive(volume, pool)
