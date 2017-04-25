from random import choice
from string import hexdigits

from common import *  # NOQA
from gdapi import ApiError
from gdapi import ClientApiError

VOLUME_CLEANUP_LABEL = 'io.rancher.container.volume_cleanup_strategy'


def test_volume_cant_delete_active(client, context):
    c = client.create_container(imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.state == 'running'

    volume = c.volumes()[0]
    assert volume.state == 'active'

    # Assert an active volume cannot be deleted
    with pytest.raises(ApiError) as e:
        client.delete(volume)
    assert e.value.error.status == 405


def test_volume_create_state(client, context):
    name = random_str()
    c = client.create_volume(name=name, driver='local')
    c = client.wait_success(c)
    assert c.state == 'inactive'

    assert c.uri == 'local:///%s' % name

    volume = client.wait_success(client.delete(c))
    assert volume.state == 'removed'


def test_volume_create_without_driver_name(client, context):
    name = random_str()
    with pytest.raises(ApiError) as e:
        client.create_volume(name=name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'


def test_volume_create_with_opts(client, context):
    name = random_str()
    c = client.create_volume(name=name,
                             driver='local',
                             driverOpts={'size': '1G'})
    c = client.wait_success(c)
    assert c.state == 'inactive'

    assert c.uri == 'local:///%s' % name

    volume = client.wait_success(client.delete(c))
    assert volume.state == 'removed'


def test_create_container_with_volume(new_context, super_client):
    client = new_context.client
    name1 = random_str()
    v1 = client.create_volume(name=name1, driver='local')
    v1 = client.wait_success(v1)
    assert v1.state == 'inactive'

    name2 = random_str()
    v2 = client.create_volume(name=name2, driver='local')
    v2 = client.wait_success(v2)
    assert v2.state == 'inactive'

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

    # Mounting happens in docker specific code; need to simulate
    create_mount(v1, c, client, super_client)
    create_mount(v2, c, client, super_client)
    v1 = client.wait_success(v1)
    v2 = client.wait_success(v2)
    assert v1.state == 'active'
    assert v2.state == 'active'

    # Assert an active volume cannot be deleted
    with pytest.raises(ApiError) as e:
        client.delete(v1)
    assert e.value.error.status == 405

    assert len(c.volumes()) == 1
    assert c.volumes()[0].id not in [v1.id, v2.id]

    vsp1 = super_client.list_volumeStoragePoolMap(volumeId=v1.id)
    vsp2 = super_client.list_volumeStoragePoolMap(volumeId=v2.id)

    assert vsp1 is not None and len(vsp1) == 1
    assert vsp2 is not None and len(vsp2) == 1

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

    assert 'must have exactly these pool(s)' in e.value.message


def create_resources(context, client, super_client, labels={}):
    vol = client.create_volume(name=random_str(), driver='local')
    unnamed_vol = client.create_volume(name=random_vol_name(), driver='local')
    data_volume_mounts = {'/con/path': vol.id, '/path2': unnamed_vol.id}
    c = client.create_container(imageUuid=context.image_uuid,
                                dataVolumeMounts=data_volume_mounts,
                                labels=labels)
    c = client.wait_success(c)
    # Simulate volume mount (only happens with real docker)
    create_mount(vol, c, client, super_client)
    create_mount(unnamed_vol, c, client, super_client)
    return c, vol, unnamed_vol


def test_instance_volume_cleanup_strategy(new_context, super_client):
    client = new_context.client

    # Assert default strategy to delete unnamed volumes only
    c, vol, unnamed_vol = create_resources(new_context, client, super_client)
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    client.wait_success(c.purge())
    wait_for_condition(client, vol, lambda x: x.state == 'detached')
    wait_for_condition(client, unnamed_vol, lambda x: x.state is not None)

    # Assert explicit 'unnamed' strategy
    c, vol, unnamed_vol = create_resources(
        new_context, client, super_client, labels={
            VOLUME_CLEANUP_LABEL: 'unnamed'})
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    client.wait_success(c.purge())
    wait_for_condition(client, vol, lambda x: x.state == 'detached')
    wait_for_condition(client, unnamed_vol, lambda x: x.state is not None)

    # Assert 'none' strategy
    c, vol, unnamed_vol = create_resources(
        new_context, client, super_client, labels={
            VOLUME_CLEANUP_LABEL: 'none'})
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    client.wait_success(c.purge())
    wait_for_condition(client, vol, lambda x: x.state == 'detached')
    wait_for_condition(client, unnamed_vol, lambda x: x.state == 'detached')

    # Assert 'all' strategy
    c, vol, unnamed_vol = create_resources(
        new_context, client, super_client, labels={
            VOLUME_CLEANUP_LABEL: 'all'})
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    client.wait_success(c.purge())
    wait_for_condition(client, vol, lambda x: x.state is not None)
    wait_for_condition(client, unnamed_vol, lambda x: x.state is not None)

    # Assert invalid value for label is rejected
    with pytest.raises(ApiError):
        create_resources(
            new_context, client, super_client,
            labels={VOLUME_CLEANUP_LABEL: 'foo'})


def create_container_and_mount(client, data_volume_mounts, new_context,
                               super_client, vols):
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumeMounts=data_volume_mounts,
                                labels={VOLUME_CLEANUP_LABEL: 'all'})
    c = client.wait_success(c)
    for vol in vols:
        c, m = create_mount(vol, c, client, super_client)
    return c


def purge_instance_and_check_volume_state(c, vols, s, client):
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    client.wait_success(c.purge())
    for vol in vols:
        wait_for_condition(client, vol,
                           lambda x: (s == 'removed' and
                                      x.removed is not None) or x.state == s,
                           lambda x: 'State: %s. Expected: %s' % (
                               x.state, s))


def create_volume_and_dvm(client, count):
    dvms = {}
    vols = []
    for i in range(0, count):
        v1 = client.create_volume(name=random_str(), driver='local')
        dvms['/con/path%s' % i] = v1.id
        vols.append(v1)

    return dvms, vols


def test_volume_remove_on_purge(new_context, super_client):
    client = new_context.client

    # Simple case: volume associated with one container that is purged
    # volume gets removed
    dvms, vols = create_volume_and_dvm(client, 2)
    c = create_container_and_mount(client, dvms, new_context,
                                   super_client, vols)
    purge_instance_and_check_volume_state(c, vols, 'removed', client)

    # Vol associated with multiple containers
    dvms, vols = create_volume_and_dvm(client, 2)
    c = create_container_and_mount(client, dvms, new_context,
                                   super_client, vols)
    c2 = create_container_and_mount(client, dvms, new_context,
                                    super_client, vols)
    purge_instance_and_check_volume_state(c, vols, 'active', client)
    purge_instance_and_check_volume_state(c2, vols, 'removed', client)


def test_volume_mounting_and_delete(new_context, super_client):
    client = new_context.client

    v1 = client.create_volume(name=random_str(), driver='local')

    data_volume_mounts = {'/con/path': v1.id}
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumeMounts=data_volume_mounts)
    c = client.wait_success(c)
    assert c.state == 'running'

    v1 = client.wait_success(v1)
    assert len(v1.storagePools()) == 1

    # Creating a mount that associates the volume to the container
    # only happens when integrating with real docker, so we'll simulate it
    c, m = create_mount(v1, c, client, super_client)

    # Assert that creating the mount results in activating volume
    check_mount_count(client, c, 1)
    assert m.state == 'active'
    v1 = wait_for_condition(client, v1, lambda x: x.state == 'active')

    # Assert that a volume with mounts cannot be deactivated, removed or purged
    assert 'deactivate' not in v1.actions and 'remove' not in v1.actions \
           and 'purge' not in v1.actions

    # Assert that once the container is removed, the mounts are removed and the
    # the volume is deactivated
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    v1 = wait_for_condition(client, v1, lambda x: x.state == 'detached')
    check_mount_count(client, c, 0)

    # Mount to new container to assert volume goes back to active
    c2 = client.create_container(imageUuid=new_context.image_uuid,
                                 dataVolumeMounts=data_volume_mounts)
    c2 = client.wait_success(c2)
    c2, m2 = create_mount(v1, c2, client, super_client)
    check_mount_count(client, c2, 1)
    v1 = wait_for_condition(client, v1, lambda x: x.state == 'active')

    # Make the volume be mounted to two containers
    c3 = client.create_container(imageUuid=new_context.image_uuid,
                                 dataVolumeMounts=data_volume_mounts,
                                 labels={VOLUME_CLEANUP_LABEL: 'all'})
    c3 = client.wait_success(c3)
    c3, m3 = create_mount(v1, c3, client, super_client)
    check_mount_count(client, c3, 1)
    check_mount_count(client, v1, 2)

    # Remove 1 one of the containers and assert that actions are still blocked
    c2 = client.wait_success(c2.stop())
    c2 = client.wait_success(c2.remove())
    check_mount_count(client, c2, 0)
    v1 = wait_for_condition(client, v1, lambda x: x.state == 'active')
    v1 = client.wait_success(v1)
    check_mount_count(client, v1, 1)
    assert 'deactivate' not in v1.actions and 'remove' not in v1.actions \
           and 'purge' not in v1.actions

    # Remove remaining container and assert the volume can be removed
    c3 = client.wait_success(c3.stop())
    c3 = client.wait_success(c3.remove())
    check_mount_count(client, c3, 0)
    wait_for_condition(client, v1, lambda x: x.removed is not None)


def test_volume_storage_pool_purge(new_context, super_client):
    client = new_context.client

    vol_name = random_str()
    v1 = client.create_volume(name=vol_name, driver='local')
    data_volume_mounts = {'/con/path': v1.id}
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumeMounts=data_volume_mounts)
    c = client.wait_success(c)
    assert c.state == 'running'

    c, m = create_mount(v1, c, client, super_client)
    check_mount_count(client, c, 1)
    assert m.state == 'active'
    v1 = wait_for_condition(client, v1, lambda x: x.state == 'active')
    sp = v1.storagePools()[0]

    host = c.hosts()[0]
    host = client.wait_success(host.deactivate())
    host = client.wait_success(host.remove())
    client.wait_success(host.purge())

    wait_for_condition(client, sp, lambda x: x.state is not None)
    wait_for_condition(client, v1, lambda x: x.state == 'detached')

    register_simulated_host(new_context)

    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumes=['%s:/foo' % vol_name])
    c = client.wait_success(c)
    assert c.state == 'running'


def create_mount(volume, container, client, super_client):
    mount = super_client.create_mount(volumeId=volume.id,
                                      instanceId=container.id,
                                      accountId=container.accountId)
    mount = super_client.wait_success(mount)
    return client.reload(container), mount


def check_mount_count(client, resource, count):
    wait_for_condition(client, resource, lambda x: len(
        [i for i in resource.mounts() if i.state != 'inactive']) == count)


def random_vol_name():
    # Emulates the random name that docker would assign to an unnamed volume
    return ''.join(choice(hexdigits) for i in range(64))
