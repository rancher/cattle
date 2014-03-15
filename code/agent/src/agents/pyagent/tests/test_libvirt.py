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
from .test_libvirt_storage import fake_volume
from dstack.plugins.libvirt import enabled

if enabled():
    from dstack.plugins.libvirt_directory_pool import DirectoryPoolDriver


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

    def post(req, resp):
        docker_container = resp['data']['instance']['+data']['dockerContainer']
        fields = resp['data']['instance']['+data']['+fields']
        del docker_container['Created']
        del docker_container['Id']
        del docker_container['Status']
        del docker_container['Ports'][0]['PublicPort']
        del docker_container['Ports'][1]['PublicPort']
        del fields['dockerIp']
        assert fields['dockerPorts']['8080/tcp'] is not None
        assert fields['dockerPorts']['12201/udp'] is not None
        fields['dockerPorts']['8080/tcp'] = '1234'
        fields['dockerPorts']['12201/udp'] = '5678'

    event_test(agent, 'libvirt/instance_activate', pre_func=pre, post_func=post)
#
#
#@if_libvirt
#def test_instance_activate_command(agent, responses):
#    _delete_container('/c-c861f990-4472-4fa1-960f-65171b544c28')
#
#    def post(req, resp):
#        docker_container = resp['data']['instance']['+data']['dockerContainer']
#        fields = resp['data']['instance']['+data']['+fields']
#        del docker_container['Created']
#        del docker_container['Id']
#        del docker_container['Status']
#        del docker_container['Ports'][0]['PublicPort']
#        del docker_container['Ports'][1]['PublicPort']
#        del fields['dockerIp']
#        assert fields['dockerPorts']['8080/tcp'] is not None
#        assert fields['dockerPorts']['12201/udp'] is not None
#        fields['dockerPorts']['8080/tcp'] = '1234'
#        fields['dockerPorts']['12201/udp'] = '5678'
#
#    event_test(agent, 'docker/instance_activate_command', post_func=post)
#
#
#@if_libvirt
#def test_instance_activate_command_args(agent, responses):
#    _delete_container('/ca-c861f990-4472-4fa1-960f-65171b544c28')
#
#    def post(req, resp):
#        docker_container = resp['data']['instance']['+data']['dockerContainer']
#        fields = resp['data']['instance']['+data']['+fields']
#        del docker_container['Created']
#        del docker_container['Id']
#        del docker_container['Status']
#        del docker_container['Ports'][0]['PublicPort']
#        del docker_container['Ports'][1]['PublicPort']
#        del fields['dockerIp']
#        assert fields['dockerPorts']['8080/tcp'] is not None
#        assert fields['dockerPorts']['12201/udp'] is not None
#        fields['dockerPorts']['8080/tcp'] = '1234'
#        fields['dockerPorts']['12201/udp'] = '5678'
#
#    event_test(agent, 'docker/instance_activate_command_args', post_func=post)
#
#
#@if_libvirt
#def test_instance_deactivate(agent, responses):
#    CONFIG_OVERRIDE['STOP_TIMEOUT'] = 1
#
#    test_instance_activate(agent, responses)
#
#    def post(req, resp):
#        del resp['data']['instance']['+data']['dockerContainer']['Created']
#        del resp['data']['instance']['+data']['dockerContainer']['Id']
#        del resp['data']['instance']['+data']['dockerContainer']['Status']
#        del resp['data']['instance']['+data']['+fields']['dockerIp']
#
#    event_test(agent, 'docker/instance_deactivate', post_func=post)
#
#

@if_libvirt
def test_ping(agent, responses):
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
