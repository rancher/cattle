from common_fixtures import *  # NOQA
from cattle import ApiError


def test_standalone_du_lifecycle(super_client, context):
    c = context.super_create_container(name=random_str())
    assert c.deploymentUnitUuid is not None
    uuid = c.deploymentUnitUuid
    wait_for(lambda: len(super_client.list_deploymentUnit(uuid=uuid)) == 1)

    du = super_client.list_deploymentUnit(uuid=c.deploymentUnitUuid)[0]

    # remove the container, validate the du is gone
    c = super_client.wait_success(c.stop())
    super_client.wait_success(c.remove())
    wait_for(lambda: super_client.reload(du).state == 'removed')


def test_adding_two_references(context):
    c1 = context.super_create_container(name=random_str())
    c2 = context.super_create_container(name=random_str())

    with pytest.raises(ApiError) as e:
        context.super_create_container(name=random_str(),
                                       networkContainerId=c1.id,
                                       dataVolumesFrom=[c2.id])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_du_multiple_instances_lifecycle(super_client, context):
    c1 = context.super_create_container(name=random_str())
    c2 = context.super_create_container(name=random_str(),
                                        networkContainerId=c1.id)
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
    c1 = context.super_create_container(name=random_str())
    data = {
        'compute.instance.activate::fail': True,
        'io.cattle.platform.process.instance.InstanceStart': {
            'computeTries': 1
        }
    }

    c2 = context.super_create_container_no_success(name=random_str(),
                                                   data=data,
                                                   networkContainerId=c1.id)

    assert c2.state == 'error'
    assert c2.transitioning == 'error'
    assert c2.deploymentUnitUuid == c1.deploymentUnitUuid

    du = super_client.list_deploymentUnit(uuid=c1.deploymentUnitUuid)[0]
    wait_for(lambda: super_client.reload(du).state == 'active')


def test_du_sidekick_to(super_client, context):
    c1 = context.super_create_container(name=random_str())
    c2 = context.super_create_container(name=random_str(),
                                        sidekickTo=c1.id)
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


def test_restart_always(context, client, super_client):
    p = {"name": "always"}
    c1 = context.create_container(name=random_str(),
                                  restartPolicy=p)
    c2 = context.create_container(name=random_str(),
                                  networkContainerId=c1.id,
                                  restartPolicy=p)
    c3 = context.create_container(name=random_str(),
                                  networkContainerId=c2.id,
                                  restartPolicy=p)

    c1 = client.wait_success(c1)
    c2 = client.wait_success(c2)
    c3 = client.wait_success(c3)

    assert c1.state == 'running'
    assert c2.state == 'running'
    assert c3.state == 'running'

    sc_1 = c1.startCount
    sc_2 = c2.startCount
    sc_3 = c3.startCount
    c1.stop(stopSource="external")

    wait_for(lambda: client.reload(c1).state == 'running')
    wait_for(lambda: client.reload(c1).startCount > sc_1)
    wait_for(lambda: client.reload(c2).startCount > sc_2)
    wait_for(lambda: client.reload(c3).startCount > sc_3)
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
    c = client.create_container(name=random_str(),
                                imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.deploymentUnitUuid is not None

    r = c.revision()
    config = r.config
    assert config.launchConfig.imageUuid == context.image_uuid
    assert config.launchConfig.version == '0'


def test_convert_to_service_primary(client, context):
    c = client.create_container(name=random_str(),
                                imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.deploymentUnitUuid is not None

    r = c.revision()
    config = r.config
    assert config.launchConfig.imageUuid == context.image_uuid
    assert config.launchConfig.version == '0'

    s = c.converttoservice()
    s = client.wait_success(s)
    assert s.state == 'active'
    assert s is not None
    assert s.name == c.name
    assert s.stackId == c.stackId
    assert s.scale == 1
    assert s.launchConfig is not None
    assert s.launchConfig.imageUuid == c.imageUuid

    c = client.wait_success(c)
    assert 'converttoservice' not in c

    # scale up the service
    s = client.update(s, scale=2)
    c = client.reload(c)
    # validate old container is intact
    assert c.state == 'running'
    assert c.serviceId == s.id
    assert c.stackId == s.stackId

    assert s.scale == 2

    # validate the new container
    stack = client.by_id('stack', s.stackId)
    _wait_until_active_map_count(s, 2, client)
    c1 = _validate_service_instance_start(client, s, stack, "2")

    s = client.wait_success(s.deactivate())
    wait_for(lambda: client.reload(c).state == 'stopped')
    wait_for(lambda: client.reload(c1).state == 'stopped')

    s = client.wait_success(s.activate())
    wait_for(lambda: client.reload(c).state == 'running')
    wait_for(lambda: client.reload(c1).state == 'running')


def test_convert_to_service_sidekicks(client, context):
    c1 = client.create_container(imageUuid=context.image_uuid,
                                 name=random_str())
    c1 = client.wait_success(c1)
    assert c1.deploymentUnitUuid is not None

    c2 = context.create_container(networkContainerId=c1.id,
                                  dataVolumesFrom=[c1.id],
                                  name=random_str())
    assert c1.deploymentUnitUuid == c2.deploymentUnitUuid

    r = c1.revision()
    config = r.config
    assert config.launchConfig.imageUuid == context.image_uuid
    assert config.launchConfig.version == '0'

    s = c1.converttoservice()
    assert s is not None

    s = client.wait_success(s)
    assert s.state == 'active'
    assert s.name == c1.name
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

    # scale up
    s = client.update(s, scale=2)
    assert client.reload(c1).state == 'running'
    assert client.reload(c2).state == 'running'

    assert s.scale == 2

    # validate the new container
    stack = client.by_id('stack', s.stackId)
    _wait_until_active_map_count(s, 4, client)
    c3 = _validate_service_instance_start(client, s, stack, "2")
    c4 = _validate_service_instance_start(client, s,
                                          stack, "2", c2.name)

    s = client.wait_success(s.deactivate())
    wait_for(lambda: client.reload(c1).state == 'stopped')
    wait_for(lambda: client.reload(c2).state == 'stopped')
    wait_for(lambda: client.reload(c3).state == 'stopped')
    wait_for(lambda: client.reload(c4).state == 'stopped')

    s = client.wait_success(s.activate())
    wait_for(lambda: client.reload(c1).state == 'running')
    wait_for(lambda: client.reload(c2).state == 'running')
    wait_for(lambda: client.reload(c3).state == 'running')
    wait_for(lambda: client.reload(c4).state == 'running')


def _wait_until_active_map_count(service, count, client):
    def wait_for_map_count(service):
        m = client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == count

    wait_for(lambda: wait_for_condition(client, service, wait_for_map_count))
    return client. \
        list_serviceExposeMap(serviceId=service.id, state='active')


def _validate_service_instance_start(client, service, env,
                                     number, launch_config_name=None):
    cn = launch_config_name + "-" if \
        launch_config_name is not None else ""
    name = env.name + "-" + service.name + "-" + cn + number

    def wait_for_map_count(service):
        instances = client. \
            list_container(name=name,
                           state="running")
        return len(instances) == 1

    wait_for(lambda: wait_for_condition(client, service,
                                        wait_for_map_count))

    instances = client. \
        list_container(name=name,
                       state="running")
    return instances[0]


def test_instance_upgrade_like_ui(client, context):
    c1 = client.create_container(name=random_str(),
                                 imageUuid=context.image_uuid)
    c1 = client.wait_success(c1)
    assert c1.deploymentUnitUuid is not None

    r1 = c1.revision()
    config = r1.config
    assert config.launchConfig['imageUuid'] == context.image_uuid
    assert config.launchConfig['version'] == '0'

    c1.imageUuid = context.image_uuid + '1'
    c1.ports = ['8080']
    c1.description = 'foo'

    # upgrade with config
    new_rev = c1.upgrade(config=c1)

    assert r1.id != new_rev.id

    def get_instance():
        instances = new_rev.instances()
        if len(instances) == 1:
            return instances[0]

    c2 = wait_for(get_instance)
    c2 = client.wait_success(c2)

    assert c2.imageUuid == c1.imageUuid
    assert c2.description == 'foo'
    assert c2.ports == ['8080']
    assert c2.labels is None
    assert c1.id != c2.id
    assert c1.deploymentUnitUuid == c2.deploymentUnitUuid
    assert c1.revisionId != c2.revisionId
    wait_for(lambda: client.reload(c1).state == 'removed')


def test_instance_upgrade(client, context):
    c1 = client.create_container(name=random_str(),
                                 privileged=True,
                                 imageUuid=context.image_uuid)
    c1 = client.wait_success(c1)
    assert c1.deploymentUnitUuid is not None

    r1 = c1.revision()
    config = r1.config
    assert config.launchConfig['imageUuid'] == context.image_uuid
    assert config.launchConfig['version'] == '0'

    config = {
        'imageUuid': context.image_uuid + '1',
        'description': 'foo',
    }

    # upgrade with config
    new_rev = c1.upgrade(config=config)

    assert r1.id != new_rev.id

    def get_instance():
        instances = new_rev.instances()
        if len(instances) == 1:
            return instances[0]

    c2 = wait_for(get_instance)
    c2 = client.wait_success(c2)

    assert c2.imageUuid == config['imageUuid']
    assert c2.description == 'foo'
    assert c1.id != c2.id
    assert c1.deploymentUnitUuid == c2.deploymentUnitUuid
    assert c1.revisionId != c2.revisionId
    assert c2.labels is None
    # Test default values
    assert c2.privileged
    wait_for(lambda: client.reload(c1).state == 'removed')


def test_standalone_upgrade_lb_replacement(client, context):
    c1 = client.create_container(name=random_str(),
                                 imageUuid=context.image_uuid)
    c1 = client.wait_success(c1)

    env = client.create_stack(name='env-' + random_str())
    launch_config = {"imageUuid": context.image_uuid}
    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule1 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "instanceId": c1.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rules = [port_rule1]
    lb_config = {"portRules": port_rules,
                 "config": config}

    # create balancer
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.lbConfig.portRules) == 1
    assert lb_svc.lbConfig.portRules[0].instanceId == c1.id

    # upgrade c1 and make sure its replacement got programmed to lb
    config = {
        'name': random_str(),
        'imageUuid': context.image_uuid + '1'
    }

    rev = c1.upgrade(config=config)
    wait_for(lambda: len(rev.instances()) == 1)
    c2 = rev.instances()[0]
    wait_for_condition(client, c2, lambda x: x.state == 'running')

    wait_for(lambda: len(client.reload(lb_svc).lbConfig.portRules) == 1)
    wait_for(lambda: client.reload(lb_svc).
             lbConfig.portRules[0].instanceId == c2.id)
