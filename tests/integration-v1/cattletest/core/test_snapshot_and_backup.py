from common_fixtures import *  # NOQA
from cattle import ApiError
from test_volume import create_mount


def test_snapshot_lifecycle(context, client, super_client):
    driver = 'driver-' + random_str()
    name = random_str()
    vol = client.create_volume(name=name, driver=driver)
    enable_snapshot(vol, super_client)

    name2 = random_str()
    vol2 = client.create_volume(name=name2, driver=driver)
    enable_snapshot(vol2, super_client)

    no_snap = 'no-snap' + name
    no_snap_vol = client.create_volume(name='no-snap' + name, driver=driver)

    c = client.create_container(imageUuid=context.image_uuid,
                                dataVolumes=[name + ':/a',
                                             name2 + ':/b',
                                             no_snap + ':/c'])
    c = client.wait_success(c)

    create_mount(vol, c, client, super_client)
    create_mount(vol2, c, client, super_client)
    create_mount(no_snap_vol, c, client, super_client)

    # Can't snapshot this volume
    with pytest.raises(AttributeError):
        no_snap_vol.snapshot()

    snap1 = take_snapshot(vol, client)
    snap2 = take_snapshot(vol, client)
    snap3 = take_snapshot(vol, client)
    snap4 = take_snapshot(vol, client)

    # should not be able to remove latest snapshot
    with pytest.raises(ApiError) as e:
        snap4.remove()
    assert e.value.error.status == 400

    # remove non-latest snapshot
    snap3 = client.wait_success(snap3.remove())
    assert snap3.state == 'removed'

    # Cannot revert a volume mounted to a running container
    with pytest.raises(AttributeError):
        vol.reverttosnapshot(snapshotId=snap2.id)

    # Container stopped, can revert
    client.wait_success(c.stop())
    vol = client.reload(vol)

    # Snapshot for volume X cannot be used to revert volume Y
    vol2_snap = take_snapshot(vol2, client)
    with pytest.raises(ApiError) as e:
        client.wait_success(vol.reverttosnapshot(snapshotId=vol2_snap.id))
    assert e.value.error.status == 422

    # Successful revert
    vol = client.wait_success(vol.reverttosnapshot(snapshotId=snap2.id))
    assert vol.state == 'active'

    # When a volume is reverted, all snapshots that are newer than the target
    # snapshot must be removed. older ones must not be removed
    wait_for_condition(client, snap4, lambda x: x.state == 'removed')
    snap1 = client.wait_success(snap1)
    assert snap1.state == 'created'


def test_backup_lifecycle(context, client, super_client):
    driver = 'driver-' + random_str()
    name = random_str()
    vol = client.create_volume(name=name, driver=driver)
    enable_snapshot(vol, super_client)

    c = client.create_container(imageUuid=context.image_uuid,
                                dataVolumes=[name + ':/a'])
    c = client.wait_success(c)

    create_mount(vol, c, client, super_client)

    snap1 = take_snapshot(vol, client)
    snap2 = take_snapshot(vol, client)
    snap3 = take_snapshot(vol, client)

    target = client.create_backup_target(name='backupTarget1')
    target = client.wait_success(target)

    backup = snap2.backup(backupTargetId=target.id)
    backup = client.wait_success(backup)

    # Cannot restore a volume mounted to a running container
    with pytest.raises(AttributeError):
        vol.restorefrombackup(backupId=backup.id)

    # Container stopped, can restore
    client.wait_success(c.stop())
    vol = client.reload(vol)
    vol = client.wait_success(vol.restorefrombackup(backupId=backup.id))
    assert vol.state == 'active'

    # When a volume is restored from a backup , all snapshots are removed
    wait_for_condition(client, snap1, lambda x: x.state == 'removed')
    wait_for_condition(client, snap2, lambda x: x.state == 'removed')
    wait_for_condition(client, snap3, lambda x: x.state == 'removed')


def test_root_volume_restore(context, client, super_client):
    driver = 'driver-' + random_str()
    name = random_str()
    # Cheating on creating a root volume by manually adding base-image opt
    root_vol1 = client.create_volume(name=name, driver=driver,
                                     driverOpts={
                                         'base-image': 'rancher/vm'})
    enable_snapshot(root_vol1, super_client)

    non_root_name = 'non-root-' + name
    non_root_vol = client.create_volume(name=non_root_name, driver=driver)
    enable_snapshot(non_root_vol, super_client)

    c = client.create_container(imageUuid=context.image_uuid,
                                dataVolumes=[name + ':/a',
                                             non_root_name + ':/b'])
    c = client.wait_success(c)

    create_mount(root_vol1, c, client, super_client)
    create_mount(non_root_vol, c, client, super_client)

    snap1 = take_snapshot(root_vol1, client)
    non_root_snap = take_snapshot(non_root_vol, client)

    target = client.create_backup_target(name='backupTarget1')
    target = client.wait_success(target)

    backup = snap1.backup(backupTargetId=target.id)
    backup = client.wait_success(backup)
    assert backup.state == 'created'

    non_root_backup = non_root_snap.backup(backupTargetId=target.id)
    non_root_backup = client.wait_success(non_root_backup)
    assert non_root_backup.state == 'created'

    # Container stopped, can restore
    client.wait_success(c.stop())
    root_vol1 = client.reload(root_vol1)
    non_root_vol = client.reload(non_root_vol)

    # If target volume is root, cannot restore across volumes
    with pytest.raises(ApiError):
        root_vol1.restorefrombackup(backupId=non_root_backup.id)

    # If source volume is root, cannot restore across volumes
    with pytest.raises(ApiError):
        non_root_vol.restorefrombackup(backupId=root_vol1.id)


def take_snapshot(vol, client):
    vol = client.wait_success(vol)
    snap = vol.snapshot()
    client.wait_success(vol)
    snap = client.wait_success(snap)
    assert snap.state == 'created'
    return snap


def enable_snapshot(volume, super_client):
    # In a real-world scenario, a volume gets the snapshot
    v = super_client.wait_success(
        super_client.update(volume, capabilities=['snapshot']))
    assert v.capabilities == ['snapshot']
