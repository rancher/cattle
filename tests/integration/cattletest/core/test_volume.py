from common_fixtures import *  # NOQA
from gdapi import ApiError
from gdapi import ClientApiError


def test_volume_delete_active(client, context):
    c = client.create_container(imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.state == 'running'

    volume = c.volumes()[0]
    assert volume.state == 'active'

    volume = client.wait_success(client.delete(volume))
    assert volume.state == 'removed'


def test_volume_create_state(client, context):
    name = random_str()
    c = client.create_volume(name=name, driver='local')
    c = client.wait_success(c)
    assert c.state == 'requested'

    assert c.uri == 'local:///%s' % name

    volume = client.wait_success(client.delete(c))
    assert volume.state == 'removed'


def test_volume_create_without_driver_name(client, context):
    name = random_str()
    with pytest.raises(ApiError) as e:
        client.create_volume(name=name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'driver'


def test_volume_create_with_opts(client, context):
    name = random_str()
    c = client.create_volume(name=name,
                             driver='local',
                             driverOpts={'size': '1G'})
    c = client.wait_success(c)
    assert c.state == 'requested'

    assert c.uri == 'local:///%s' % name

    volume = client.wait_success(client.delete(c))
    assert volume.state == 'removed'


def test_create_container_with_volume(new_context, super_client):
    client = new_context.client
    name1 = random_str()
    v1 = client.create_volume(name=name1, driver='local')
    v1 = client.wait_success(v1)
    assert v1.state == 'requested'

    name2 = random_str()
    v2 = client.create_volume(name=name2, driver='local')
    v2 = client.wait_success(v2)
    assert v2.state == 'requested'

    dataVolumeMounts = {'/var/lib/docker/mntpt1': v1.id,
                        '/var/lib/docker/mntpt2': v2.id}
    dataVolumes = {'/home': '/home'}
    c = client.create_container(imageUuid=new_context.image_uuid,
                                volumeDriver='local',
                                dataVolumes=dataVolumes,
                                dataVolumeMounts=dataVolumeMounts)
    c = client.wait_success(c, timeout=240)
    assert c.state == 'running'

    dataVol1 = '%s:/var/lib/docker/mntpt1' % name1
    dataVol2 = '%s:/var/lib/docker/mntpt2' % name2

    dataVol1Found = False
    dataVol2Found = False

    for dataVol in c.dataVolumes:
        if dataVol == dataVol1:
            dataVol1Found = True
        if dataVol == dataVol2:
            dataVol2Found = True

    assert dataVol1Found and dataVol2Found

    v1 = client.wait_success(v1)
    v2 = client.wait_success(v2)

    assert v1.state == 'active'
    assert v2.state == 'active'

    assert len(c.volumes()) == 1
    assert c.volumes()[0].id not in [v1.id, v2.id]

    vsp1 = super_client.list_volumeStoragePoolMap(volumeId=v1.id)
    vsp2 = super_client.list_volumeStoragePoolMap(volumeId=v2.id)

    assert vsp1 is not None
    assert vsp2 is not None

    assert len(vsp1) == 1
    assert len(vsp2) == 1

    spid1 = vsp1[0].storagePoolId
    spid2 = vsp2[0].storagePoolId

    host1 = super_client.list_storagePoolHostMap(storagePoolId=spid1)
    host2 = super_client.list_storagePoolHostMap(storagePoolId=spid2)

    assert host1[0].id == host2[0].id

    new_host = register_simulated_host(new_context)

    with pytest.raises(ClientApiError) as e:
        c = client.create_container(imageUuid=new_context.image_uuid,
                                    volumeDriver='local',
                                    dataVolumes=dataVolumes,
                                    requestedHostId=new_host.id,
                                    dataVolumeMounts=dataVolumeMounts)
        client.wait_success(c)

    assert e.value.message == 'Failed to find a placement'
