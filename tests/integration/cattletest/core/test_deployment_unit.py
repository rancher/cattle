from common_fixtures import *  # NOQA
from cattle import ApiError


def test_standalone_du_lifecycle(super_client, context):
    c = context.super_create_container()
    assert c.deploymentUnitUuid is not None
    uuid = c.deploymentUnitUuid
    wait_for(lambda: len(super_client.list_deploymentUnit(uuid=uuid)) == 1)

    du = super_client.list_deploymentUnit(uuid=c.deploymentUnitUuid)[0]

    # remove the container, validate the du is gone
    c = super_client.wait_success(c.stop())
    super_client.wait_success(c.remove())
    wait_for(lambda: super_client.reload(du).state == 'removed')


def test_adding_two_references(context):
    c1 = context.super_create_container()
    c2 = context.super_create_container()

    with pytest.raises(ApiError) as e:
        context.super_create_container(networkContainerId=c1.id,
                                       dataVolumesFrom=[c2.id])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_du_multiple_instances_lifecycle(super_client, context):
    c1 = context.super_create_container()
    c2 = context.super_create_container(networkContainerId=c1.id)
    assert c2.deploymentUnitUuid == c1.deploymentUnitUuid

    du = super_client.list_deploymentUnit(uuid=c1.deploymentUnitUuid)[0]

    # remove c1, validate du is still intact
    c2 = super_client.wait_success(c2.stop())
    super_client.wait_success(c2.remove())
    wait_for(lambda: super_client.reload(du).state == 'active')

    # remove c2, validate du is gone
    c1 = super_client.wait_success(c1.stop())
    super_client.wait_success(c1.remove())
    wait_for(lambda: super_client.reload(du).state == 'removed')


def test_container_compute_fail(super_client, context):
    c1 = context.super_create_container()
    data = {
        'compute.instance.activate::fail': True,
        'io.cattle.platform.process.instance.InstanceStart': {
            'computeTries': 1
        }
    }

    c2 = context.super_create_container_no_success(data=data,
                                                   networkContainerId=c1.id)

    assert c2.transitioning == 'error'
    assert c2.deploymentUnitUuid == c1.deploymentUnitUuid

    du = super_client.list_deploymentUnit(uuid=c2.deploymentUnitUuid)[0]
    wait_for(lambda: super_client.reload(du).state == 'active')


def test_du_sidekick_to(super_client, context):
    c1 = context.super_create_container()
    c2 = context.super_create_container(sidekickTo=c1.id)
    assert c2.deploymentUnitUuid == c1.deploymentUnitUuid
    assert c2.sidekickTo == c1.id

    du = super_client.list_deploymentUnit(uuid=c1.deploymentUnitUuid)[0]

    # remove c1, validate du is still intact
    c2 = super_client.wait_success(c2.stop())
    super_client.wait_success(c2.remove())
    wait_for(lambda: super_client.reload(du).state == 'active')

    # remove c2, validate du is gone
    c1 = super_client.wait_success(c1.stop())
    super_client.wait_success(c1.remove())
    wait_for(lambda: super_client.reload(du).state == 'removed')


def test_restart_always(context, super_client):
    p = {"name": "always"}
    c1 = context.super_create_container(restartPolicy=p)
    c2 = context.super_create_container(networkContainerId=c1.id,
                                        restartPolicy=p)
    c3 = context.super_create_container(networkContainerId=c2.id,
                                        restartPolicy=p)

    sc_1 = c1.startCount
    sc_2 = c2.startCount
    sc_3 = c3.startCount
    c1.stop(stopSource="external")

    wait_for(lambda: super_client.reload(c1).state == 'running')
    wait_for(lambda: super_client.reload(c1).startCount > sc_1)
    wait_for(lambda: super_client.reload(c2).startCount > sc_2)
    wait_for(lambda: super_client.reload(c3).startCount > sc_3)
    assert super_client.reload(c1).stopSource is None
    assert super_client.reload(c2).stopSource is None
    assert super_client.reload(c3).stopSource is None


def test_restart_on_failure_zero_exit(context, super_client):
    p = {"maximumRetryCount": 2, "name": "on-failure"}
    c1 = context.super_create_container(restartPolicy=p, name=random_str())
    c1 = super_client.wait_success(c1.stop(stopSource="external"))

    wait_for(lambda: super_client.reload(c1).state == 'stopped')


def test_restart_on_failure_running(context, super_client):
    p = {"maximumRetryCount": 2, "name": "on-failure"}
    c1 = context.super_create_container(restartPolicy=p, name=random_str())
    sc_1 = c1.startCount
    assert c1.exitCode == 0
    super_client.update(c1, exitCode=1)
    c1 = super_client.wait_success(c1.stop(stopSource="external"))
    wait_for(lambda: super_client.reload(c1).state == 'running')
    wait_for(lambda: super_client.reload(c1).startCount > sc_1)


def test_restart_on_failure_exceed_retry(context, super_client):
    p = {"maximumRetryCount": 2, "name": "on-failure"}
    c1 = context.super_create_container(restartPolicy=p, name=random_str())
    assert c1.exitCode == 0
    super_client.update(c1, exitCode=1, startRetryCount=2)
    c1 = super_client.wait_success(c1.stop(stopSource="external"))
    wait_for(lambda: super_client.reload(c1).state == 'stopped')


def test_instance_revision(client, context):
    c = client.create_container(imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.deploymentUnitUuid is not None

    rs = client.list_instanceRevision(instanceId=c.id)
    assert len(rs) == 1
    r = rs[0]
    spec = r.specs[c.uuid]
    assert spec['imageUuid'] == context.image_uuid
    assert spec['version'] == '0'


def test_convert_to_service_primary(client, context):
    c = client.create_container(imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.deploymentUnitUuid is not None

    rs = client.list_instanceRevision(instanceId=c.id)
    assert len(rs) == 1
    r = rs[0]
    spec = r.specs[c.uuid]
    assert spec['imageUuid'] == context.image_uuid
    assert spec['version'] == '0'

    s_name = random_str()
    s = c.converttoservice(name=s_name)
    assert s is not None
    assert s.name == s_name
    assert s.stackId == c.stackId
    assert s.scale == 1
    assert s.launchConfig is not None
    assert s.launchConfig.imageUuid == c.imageUuid


def test_convert_to_service_sidekicks(client, context):
    c1 = client.create_container(imageUuid=context.image_uuid,
                                 name=random_str())
    c1 = client.wait_success(c1)
    assert c1.deploymentUnitUuid is not None

    c2 = context.super_create_container(networkContainerId=c1.id,
                                        dataVolumesFrom=[c1.id],
                                        name=random_str())
    assert c1.deploymentUnitUuid == c2.deploymentUnitUuid

    rs = client.list_instanceRevision(instanceId=c1.id)
    assert len(rs) == 1
    r = rs[0]
    spec = r.specs[c1.uuid]
    assert spec['imageUuid'] == context.image_uuid
    assert spec['version'] == '0'

    s_name = random_str()
    s = c1.converttoservice(name=s_name)
    assert s is not None

    s = client.wait_success(s)
    assert s.state == 'inactive'
    assert s.name == s_name
    assert s.stackId == c1.stackId
    assert s.revisionId is not None

    assert s.launchConfig is not None
    assert s.launchConfig.imageUuid == c1.imageUuid
    assert "name" not in s.launchConfig

    assert s.secondaryLaunchConfigs is not None
    assert len(s.secondaryLaunchConfigs) == 1
    sc = s.secondaryLaunchConfigs[0]
    assert sc.name == c2.name
    assert sc.imageUuid == context.image_uuid
    assert "networkContainerId" not in sc
    assert "networkLaunchConfig" in sc
    assert sc.networkLaunchConfig == s.name
    assert "dataVolumesFrom" not in sc
    assert "dataVolumesFromLaunchConfigs" in sc
    assert sc.dataVolumesFromLaunchConfigs == [s.name]

    assert client.reload(c1).serviceId == s.id
    assert client.reload(c2).serviceId == s.id
