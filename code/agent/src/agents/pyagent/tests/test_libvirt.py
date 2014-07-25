from xml.etree import ElementTree

from .common_fixtures import *  # NOQA
from cattle import CONFIG_OVERRIDE
from cattle.progress import LogProgress
from .common_libvirt_fixtures import *  # NOQA
from cattle.plugins.libvirt import enabled

DATA_TAG = '{http://cattle.io/schemas/cattle-libvirt}data'
DATA_NAME = '{http://cattle.io/schemas/cattle-libvirt}name'


if enabled():
    import libvirt
    from cattle.plugins.libvirt_directory_pool import DirectoryPoolDriver
    from cattle.plugins.libvirt.utils import get_preferred_libvirt_type
    from cattle.plugins.libvirt.utils import read_vnc_info
    CONFIG_OVERRIDE['HOME'] = SCRATCH_DIR
    CONFIG_OVERRIDE['LIBVIRT_HOST_IP'] = '1.2.3.4'


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

        assert int(resp['data']['+data']['libvirt']['actual-size']) > 200000
        del resp['data']['+data']['libvirt']['actual-size']
        try:
            del resp['data']['+data']['libvirt']['format-specific']
        except KeyError:
            pass

    event_test(agent, 'libvirt/image_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_image_activate_gz(random_qcow2_gz, pool_dir, agent, responses):
    def pre(req):
        req['data']['imageStoragePoolMap']['image'] = \
            fake_image(random_qcow2_gz)
        req['data']['imageStoragePoolMap']['storagePool'] = fake_pool(pool_dir)

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        del resp['data']['+data']['libvirt']['filename']

        assert int(resp['data']['+data']['libvirt']['actual-size']) > 200000
        del resp['data']['+data']['libvirt']['actual-size']
        try:
            del resp['data']['+data']['libvirt']['format-specific']
        except KeyError:
            pass

    event_test(agent, 'libvirt/image_activate', pre_func=pre, post_func=post)


@if_libvirt
def test_image_activate_bz2(random_qcow2_bz2, pool_dir, agent, responses):
    def pre(req):
        req['data']['imageStoragePoolMap']['image'] = \
            fake_image(random_qcow2_bz2)
        req['data']['imageStoragePoolMap']['storagePool'] = fake_pool(pool_dir)

    def post(req, resp):
        assert resp['data']['+data']['libvirt']['filename'].endswith('.qcow2')
        del resp['data']['+data']['libvirt']['filename']

        assert int(resp['data']['+data']['libvirt']['actual-size']) > 200000
        del resp['data']['+data']['libvirt']['actual-size']
        try:
            del resp['data']['+data']['libvirt']['format-specific']
        except KeyError:
            pass

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

        assert int(resp['data']['+data']['libvirt']['actual-size']) > 200000
        del resp['data']['+data']['libvirt']['actual-size']
        try:
            del resp['data']['+data']['libvirt']['format-specific']
        except KeyError:
            pass

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
def test_instance_activate_metadata(random_qcow2, pool_dir, agent, responses):
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
        data = resp['data']['instance']['+data']
        xml = data['+libvirt']['xml']

        assert xml is not None

        doc = ElementTree.fromstring(xml)

        for child in doc.findall('devices/disk'):
            if child.attrib['device'] == 'cdrom':
                for node in child.getchildren():
                    if node.tag == 'source' and \
                            os.path.exists(node.attrib['file']):
                        break
                else:
                    assert False
                break
        else:
            assert False

        data['+libvirt']['xml'] = '<xml/>'
        data['+fields']['libvirtVncAddress'] = '0.0.0.0:5900'
        data['+fields']['libvirtVncPassword'] = 'passwd'

    event_test(agent, 'libvirt/instance_activate_metadata', pre_func=pre,
               post_func=post)


@if_libvirt
def test_instance_activate(random_qcow2, pool_dir, agent, responses):
    _test_instance_activate(random_qcow2, pool_dir, agent, responses)
    _delete_instance('c861f990-4472-4fa1-960f-65171b544c28')


def _test_instance_activate(random_qcow2, pool_dir, agent, responses):
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
        data = resp['data']['instance']['+data']
        vnc_host = data['+fields']['libvirtVncAddress']
        passwd = data['+fields']['libvirtVncPassword']
        xml = data['+libvirt']['xml']

        assert xml is not None

        xml_host, xml_port, xml_passwd = read_vnc_info(xml)

        assert vnc_host is not None
        assert passwd is not None
        assert len(passwd) == 64

        assert not vnc_host.startswith('0.0.0.0')

        assert passwd == xml_passwd

        data['+libvirt']['xml'] = '<xml/>'
        data['+fields']['libvirtVncAddress'] = '0.0.0.0:5900'
        data['+fields']['libvirtVncPassword'] = 'passwd'

    event_test(agent, 'libvirt/instance_activate', pre_func=pre,
               post_func=post)


@if_libvirt
def test_instance_custom_template(random_qcow2, pool_dir, agent, responses):
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
        assert 'spice' in resp['data']['instance']['+data']['+libvirt']['xml']
        resp['data']['instance']['+data']['+libvirt']['xml'] = '<xml/>'

    event_test(agent, 'libvirt/instance_activate_template', pre_func=pre,
               post_func=post)

    _delete_instance('c861f990-4472-4fa1-960f-65171b544c28')


@if_libvirt
def test_instance_deactivate(random_qcow2, pool_dir, agent, responses):
    CONFIG_OVERRIDE['STOP_TIMEOUT'] = 1

    test_instance_activate(random_qcow2, pool_dir, agent, responses)

    def post(req, resp):
        pass

    event_test(agent, 'libvirt/instance_deactivate', post_func=post)


@if_libvirt
def test_ping(random_qcow2, pool_dir, agent, responses):
    _test_instance_activate(random_qcow2, pool_dir, agent, responses)

    CONFIG_OVERRIDE['DOCKER_ENABLED'] = 'false'
    CONFIG_OVERRIDE['HOSTNAME'] = 'localhost'
    CONFIG_OVERRIDE['LIBVIRT_UUID'] = 'testuuid'
    CONFIG_OVERRIDE['PHYSICAL_HOST_UUID'] = 'hostuuid'

    def post(req, resp):
        resources = resp['data']['resources']

        instances = filter(lambda x: x['type'] == 'instance' and
                           x['uuid'] == 'c861f990-4472-4fa1-960f-65171b544c28',
                           resources)
        resources = filter(lambda x: x.get('kind') == 'libvirt', resources)

        assert len(instances) == 1

        resources.append(instances[0])
        resp['data']['resources'] = resources

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

        assert resp['data']['resources'][0]['data']['libvirt']['type'] in \
            ['kvm', 'qemu']

        resp['data']['resources'][0]['data']['libvirt']['type'] = 'qemu'

    event_test(agent, 'libvirt/ping', post_func=post)
    _delete_instance('c861f990-4472-4fa1-960f-65171b544c28')


@if_libvirt
def test_preferred_libvirt_type():
    type = get_preferred_libvirt_type()
    assert type in ['qemu', 'kvm']
