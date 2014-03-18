from docker import Client
import socket

#TODO dstack.plugins.load_plugins() somehow make dstack.plugin.* modules
#  unavailable, importing it first
import dstack.plugins.docker  # NOQA

from .common_fixtures import *  # NOQA
import pytest
from dstack import CONFIG_OVERRIDE, Config
from dstack.progress import LogProgress
from .test_libvirt_storage import if_libvirt, QCOW_TEST_FILE, fake_image
from .test_libvirt_storage import random_qcow2, pool_dir, fake_pool
from .test_libvirt_storage import random_qcow2_gz, random_qcow2_bz2
from .test_libvirt_storage import fake_volume
from dstack.plugins.libvirt import enabled

if enabled():
    import libvirt
    from dstack.plugins.libvirt_directory_pool import DirectoryPoolDriver
    CONFIG_OVERRIDE['HOME'] = SCRATCH_DIR


def _delete_instance(name):
    conn = libvirt.open('qemu:///system')
    for c in conn.listAllDomains():
        if c.name() == name:
            c.destroy()


@if_libvirt
def test_image_activate(random_qcow2, pool_dir, agent, responses):
    def pre(req):
        req['data']['imageStoragePoolMap']['image'] = fake_image(random_qcow2)
        req['data']['imageStoragePoolMap']['storagePool'] = fake_pool(pool_dir)

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        del resp['data']['+data']['libvirt']['filename']

    event_test(agent, 'libvirt/image_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_image_activate_gz(random_qcow2_gz, pool_dir, agent, responses):
    def pre(req):
        req['data']['imageStoragePoolMap']['image'] = fake_image(random_qcow2_gz)
        req['data']['imageStoragePoolMap']['storagePool'] = fake_pool(pool_dir)

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        del resp['data']['+data']['libvirt']['filename']

    event_test(agent, 'libvirt/image_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_image_activate_bz2(random_qcow2_bz2, pool_dir, agent, responses):
    def pre(req):
        req['data']['imageStoragePoolMap']['image'] = fake_image(random_qcow2_bz2)
        req['data']['imageStoragePoolMap']['storagePool'] = fake_pool(pool_dir)

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        del resp['data']['+data']['libvirt']['filename']

    event_test(agent, 'libvirt/image_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_volume_activate(random_qcow2, pool_dir, agent, responses):
    volume = fake_volume(image_file=random_qcow2)
    image = volume.image
    pool = fake_pool(pool_dir)

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())

    def pre(req):
        req['data']['volumeStoragePoolMap']['volume'] = volume
        req['data']['volumeStoragePoolMap']['storagePool'] = pool

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        assert resp['data']['+data']['libvirt']['backing-filename']\
            .endswith('.qcow2')
        assert resp['data']['+data']['libvirt']['full-backing-filename']\
            .endswith('.qcow2')

        del resp['data']['+data']['libvirt']['filename']
        del resp['data']['+data']['libvirt']['backing-filename']
        del resp['data']['+data']['libvirt']['full-backing-filename']

    event_test(agent, 'libvirt/volume_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_volume_deactivate(random_qcow2, pool_dir, agent, responses):
    volume = fake_volume(image_file=random_qcow2)
    pool = fake_pool(pool_dir)

    def pre(req):
        req['data']['volumeStoragePoolMap']['volume'] = volume
        req['data']['volumeStoragePoolMap']['storagePool'] = pool

    event_test(agent, 'libvirt/volume_deactivate', pre_func=pre)


@if_libvirt
def test_instance_activate(random_qcow2, pool_dir, agent, responses):
    _delete_instance('c861f990-4472-4fa1-960f-65171b544c28')

    volume = fake_volume(image_file=random_qcow2)
    image = volume.image
    pool = fake_pool(pool_dir)
    volume['storagePools'] = [pool]

    driver = DirectoryPoolDriver()
    driver.image_activate(image, pool, LogProgress())
    driver.volume_activate(volume, pool, LogProgress())

    def pre(req):
        req.data.instanceHostMap.instance.image = image
        req.data.instanceHostMap.instance.volumes.append(volume)

    def post(_, resp):
        assert resp['data']['instance']['+data']['+libvirt']['xml'] is not None
        resp['data']['instance']['+data']['+libvirt']['xml'] = '<xml/>'

    event_test(agent, 'libvirt/instance_activate', pre_func=pre, post_func=post)

    _delete_instance('c861f990-4472-4fa1-960f-65171b544c28')


@if_libvirt
def test_instance_deactivate(random_qcow2, pool_dir, agent, responses):
    CONFIG_OVERRIDE['STOP_TIMEOUT'] = 1

    test_instance_activate(random_qcow2, pool_dir, agent, responses)

    def post(req, resp):
        pass

    event_test(agent, 'libvirt/instance_deactivate', post_func=post)



@if_libvirt
def test_ping(agent, responses):
    CONFIG_OVERRIDE['DOCKER_ENABLED'] = 'false'
    CONFIG_OVERRIDE['HOSTNAME'] = 'localhost'
    CONFIG_OVERRIDE['LIBVIRT_UUID'] = 'testuuid'

    def post(req, resp):
        resp['data']['resources'] = filter(lambda x: x['kind'] == 'libvirt',
                                           resp['data']['resources'])
        assert resp['data']['resources'][1]['name'] == \
            resp['data']['resources'][0]['name'] + ' Storage Pool ' + \
            resp['data']['resources'][1]['data']['libvirt']['poolPath']

        resp['data']['resources'][1]['name'] = \
            resp['data']['resources'][0]['name'] + ' Storage Pool'

        resp['data']['resources'][1]['data']['libvirt']['poolPath'] = \
            'pool path'

        assert resp['data']['resources'][1]['uuid'].startswith(
            resp['data']['resources'][0]['uuid'] + '-')

        resp['data']['resources'][1]['uuid'] = 'testuuid-pool'

    event_test(agent, 'libvirt/ping', post_func=post)
