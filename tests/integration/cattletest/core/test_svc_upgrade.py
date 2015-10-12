from common_fixtures import *  # NOQA
from cattle import ApiError


def create_env_and_services(context, client, from_scale=1, to_scale=1):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'

    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    scale=from_scale,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    service = client.wait_success(service.activate(), timeout=120)
    assert service.state == 'active'

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     scale=to_scale,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)
    service2 = client.wait_success(service2.activate(), timeout=120)
    assert service2.state == 'active'

    return service, service2, env


def run_upgrade(context, client, from_scale, to_scale, **kw):
    service, service2, env = create_env_and_services(context, client,
                                                     from_scale, to_scale)

    kw['toServiceId'] = service2.id
    service = service.upgrade_action(**kw)
    assert service.state == 'upgrading'

    def upgrade_not_null():
        s = client.reload(service)
        if s.upgrade is not None:
            return s

    service = wait_for(upgrade_not_null)

    service = client.wait_success(service, timeout=120)
    assert service.state == 'active'
    assert service.scale == 0
    assert service.upgrade is None

    service2 = client.wait_success(service2)
    assert service2.state == 'active'
    assert service2.scale == kw['finalScale']
    assert service2.upgrade is None


def test_upgrade_simple(context, client):
    run_upgrade(context, client, 1, 1,
                finalScale=2,
                intervalMillis=100)


def test_upgrade_odd_numbers(context, client):
    run_upgrade(context, client, 5, 2,
                batchSize=100,
                finalScale=3,
                intervalMillis=100)


def test_upgrade_to_too_high(context, client):
    run_upgrade(context, client, 1, 5,
                batchSize=2,
                finalScale=2,
                intervalMillis=100)


def test_upgrade_relink(context, client):
    service, service2, env = create_env_and_services(context, client)

    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid}

    source = client.create_service(name=random_str(),
                                   environmentId=env.id,
                                   scale=1,
                                   launchConfig=launch_config)

    lb = client.create_load_balancer_service(name=random_str(),
                                             environmentId=env.id,
                                             scale=1,
                                             launchConfig={'ports': ['80']})

    source = client.wait_success(client.wait_success(source).activate())
    assert source.state == 'active'

    lb = client.wait_success(client.wait_success(lb).activate())
    assert lb.state == 'active'

    service_link = {
        "serviceId": service.id,
        "name": "link1",
        "ports": ["a.com:1234"],
    }

    source.setservicelinks(serviceLinks=[service_link])
    lb.setservicelinks(serviceLinks=[service_link])

    source = client.wait_success(source)
    assert source.state == 'active'
    lb = client.wait_success(lb)
    assert lb.state == 'active'

    assert len(source.consumedservices()) == 1
    assert len(lb.consumedservices()) == 1
    assert len(service.consumedbyservices()) == 2
    assert len(service2.consumedbyservices()) == 0

    service = service.upgrade_action(finalScale=1,
                                     toServiceId=service2.id,
                                     updateLinks=True)
    service = client.wait_success(service, timeout=120)
    assert service.state == 'active'

    assert len(source.consumedservices()) == 2
    assert len(lb.consumedservices()) == 2
    assert len(service.consumedbyservices()) == 2
    assert len(service2.consumedbyservices()) == 2

    links = client.list_service_consume_map(serviceId=lb.id)
    assert len(links) == 2

    for link in links:
        assert link.ports == ["a.com:1234"]


def _create_multi_lc_svc(super_client, client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}
    secondary_lc1 = {"imageUuid": image_uuid, "name": "secondary1",
                     "dataVolumesFromLaunchConfigs": ['secondary2']}
    secondary_lc2 = {"imageUuid": image_uuid, "name": "secondary2"}
    secondary = [secondary_lc1, secondary_lc2]
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                scale=2,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=secondary)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate(), timeout=120)
    assert svc.state == 'active'
    c11, c11_sec1, c11_sec2, c12, c12_sec1, c12_sec2 \
        = _get_containers(super_client, svc)
    assert svc.launchConfig.version == '0'
    assert svc.secondaryLaunchConfigs[0].version == '0'
    assert svc.secondaryLaunchConfigs[1].version == '0'
    assert c11.version == svc.launchConfig.version
    assert c12.version == svc.launchConfig.version
    assert c11_sec1.version == svc.secondaryLaunchConfigs[0].version
    assert c12_sec1.version == svc.secondaryLaunchConfigs[0].version
    assert c11_sec2.version == svc.secondaryLaunchConfigs[1].version
    assert c12_sec2.version == svc.secondaryLaunchConfigs[1].version

    return env, svc


def _get_containers(super_client, service):
    i_maps = _wait_for_map_count(super_client, service, 2)

    c11 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    i_maps = _wait_for_map_count(super_client, service, 2, 'secondary1')
    c11_sec1 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12_sec1 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    i_maps = _wait_for_map_count(super_client, service, 2, 'secondary2')
    c11_sec2 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12_sec2 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    return c11, c11_sec1, c11_sec2, c12, c12_sec1, c12_sec2


def _validate_upgrade(super_client, upgraded_svc,
                      primary='0', secondary1='0', secondary2='0'):
    # validate number of upgraded instances first
    _validate_upgraded_instances_count(super_client,
                                       upgraded_svc,
                                       primary, secondary1, secondary2)

    c21, c21_sec1, c21_sec2, c22, c22_sec1, c22_sec2 = \
        _get_containers(super_client, upgraded_svc)
    assert upgraded_svc.launchConfig.version == primary
    assert upgraded_svc.secondaryLaunchConfigs[0].version == secondary1
    assert upgraded_svc.secondaryLaunchConfigs[1].version == secondary2
    assert c21.version == upgraded_svc.launchConfig.version
    assert c22.version == upgraded_svc.launchConfig.version
    assert c21_sec1.version == upgraded_svc.secondaryLaunchConfigs[0].version
    assert c22_sec1.version == upgraded_svc.secondaryLaunchConfigs[0].version
    assert c21_sec2.version == upgraded_svc.secondaryLaunchConfigs[1].version
    assert c22_sec2.version == upgraded_svc.secondaryLaunchConfigs[1].version


def test_in_service_upgrade_primary(context, client, super_client):
    env, svc = _inservice_upgrade(context,
                                  client, super_client,
                                  launchConfig={'labels': {'foo': "bar"}})
    _validate_upgrade(super_client, svc, primary='1',
                      secondary1='0', secondary2='0')


def test_in_service_upgrade_all(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}},
                 {'name': "secondary2", 'labels': {'foo': "bar"}}]
    env, svc = _inservice_upgrade(context, client,
                                  super_client,
                                  launchConfig={'labels': {'foo': "bar"}},
                                  secondaryLaunchConfigs=secondary,
                                  batchSize=3,
                                  intervalMillis=200)
    _validate_upgrade(super_client, svc,
                      primary='1', secondary1='1', secondary2='1')


def test_in_service_upgrade_one_secondary(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, upgraded_svc = _inservice_upgrade(context, client, super_client,
                                           secondaryLaunchConfigs=secondary,
                                           batchSize=2,
                                           intervalMillis=100)
    _validate_upgrade(super_client, upgraded_svc,
                      primary='0', secondary1='1', secondary2='0')


def test_in_service_upgrade_mix(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, svc = _inservice_upgrade(context, client, super_client,
                                  launchConfig={'labels': {'foo': "bar"}},
                                  secondaryLaunchConfigs=secondary,
                                  batchSize=1)
    _validate_upgrade(super_client, svc,
                      primary='1', secondary1='1', secondary2='0')


def test_upgrade_param_validation(context, client):
    # test mixed regular and in service upgrade
    launch_config = {'networkMode': None}
    with pytest.raises(ApiError) as e:
        run_upgrade(context, client, 1, 1,
                    finalScale=2,
                    intervalMillis=100,
                    launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_upgrade_invalid_config(context, client, super_client):
    # pass invalid config name
    image_uuid = context.image_uuid
    secondary_invalid = {"imageUuid": image_uuid, "name": "secondary3"}
    with pytest.raises(ApiError) as e:
        _inservice_upgrade(context, client, super_client,
                           launchConfig={'labels': {'foo': "bar"}},
                           secondaryLaunchConfigs=secondary_invalid,
                           batchSize=1)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_big_scale(context, client):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}

    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                scale=15,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate(), timeout=120)
    svc = svc.upgrade_action(batchSize=1,
                             launchConfig={'labels': {'foo': "bar"}})
    svc = client.wait_success(svc, 120)
    svc = svc.upgrade_action(batchSize=5,
                             launchConfig={'labels': {'foo': "bar"}})
    client.wait_success(svc, 120)


def _inservice_upgrade(context, client, super_client, **kw):
    env, svc = _create_multi_lc_svc(super_client, client, context)

    svc = svc.upgrade_action(**kw)
    assert svc.state == 'upgrading'

    def upgrade_not_null():
        s = client.reload(svc)
        if s.upgrade is not None:
            return s

    svc = wait_for(upgrade_not_null)

    svc = client.wait_success(svc, timeout=120)
    assert svc.state == 'active'
    return env, svc


def _wait_for_instance_start(super_client, id):
    wait_for(
        lambda: len(super_client.by_id('container', id)) > 0
    )
    return super_client.by_id('container', id)


def _wait_for_map_count(super_client, service, count, launchConfig=None):
    def get_active_launch_config_instances():
        match = []
        instance_maps = super_client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        for instance_map in instance_maps:
            if launchConfig is not None:
                if instance_map.dnsPrefix == launchConfig:
                    match.append(instance_map)
            else:
                if instance_map.dnsPrefix is None:
                    match.append(instance_map)
        return match

    def active_len():
        match = get_active_launch_config_instances()
        if len(match) == count:
            return match

    wait_for(active_len)
    return get_active_launch_config_instances()


def _validate_upgraded_instances_count(super_client,
                                       upgraded_service,
                                       primary='0',
                                       secondary1='0', secondary2='0'):
    count = 0
    if primary == "1":
        count += 2
    if secondary1 == "1":
        count += 2
    if secondary2 == "1":
        count += 2

    def get_upgraded_instances():
        match = []
        instances = super_client. \
            list_container(state='running',
                           accountId=upgraded_service.accountId)
        for instance in instances:
            if upgraded_service.name in \
                    instance.name and instance.version == "1":
                labels = {'foo': "bar"}
                assert all(item in instance.labels for item in labels) is True
                match.append(instance)
        return match

    def active_len():
        match = get_upgraded_instances()
        if len(match) == count:
            return match

    wait_for(active_len)
