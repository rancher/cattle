from common_fixtures import *  # NOQA

DEFAULT_TIMEOUT = 120


def test_upgrade_simple(context, client):
    _run_upgrade(context, client, 1, 1,
                 finalScale=2,
                 intervalMillis=100)


def test_upgrade_odd_numbers(context, client):
    _run_upgrade(context, client, 5, 2,
                 batchSize=100,
                 finalScale=3,
                 intervalMillis=100)


def test_upgrade_to_too_high(context, client):
    _run_upgrade(context, client, 1, 5,
                 batchSize=2,
                 finalScale=2,
                 intervalMillis=100)


def test_upgrade_relink(context, client):
    service, service2, env = _create_env_and_services(context, client)

    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid}

    source = client.create_service(name=random_str(),
                                   stackId=env.id,
                                   scale=1,
                                   launchConfig=launch_config)

    target = client.create_service(name=random_str(),
                                   stackId=env.id,
                                   scale=1,
                                   launchConfig=launch_config)

    source = client.wait_success(client.wait_success(source).activate())
    assert source.state == 'active'

    target = client.wait_success(client.wait_success(target).activate())
    assert target.state == 'active'

    service_link = {
        "serviceId": service.id,
        "name": "link1",
    }

    source.setservicelinks(serviceLinks=[service_link])
    target.setservicelinks(serviceLinks=[service_link])

    source = client.wait_success(source)
    assert source.state == 'active'
    target = client.wait_success(target)
    assert target.state == 'active'

    assert len(source.consumedservices()) == 1
    assert len(target.consumedservices()) == 1
    assert len(service.consumedbyservices()) == 2
    assert len(service2.consumedbyservices()) == 0

    strategy = {"finalScale": 1,
                "toServiceId": service2.id,
                "updateLinks": True,
                "intervalMillis": 100}
    service = service.upgrade_action(toServiceStrategy=strategy)
    service = client.wait_success(service, timeout=DEFAULT_TIMEOUT)
    assert service.state == 'upgraded'

    assert len(source.consumedservices()) == 2
    assert len(target.consumedservices()) == 2
    assert len(service.consumedbyservices()) == 2
    assert len(service2.consumedbyservices()) == 2

    links = client.list_service_consume_map(serviceId=target.id)
    assert len(links) == 2


def test_in_service_upgrade_primary(context, client, super_client):
    env, svc, up_svc = _insvc_upgrade(context,
                                      client, super_client, True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      startFirst=True)
    _validate_upgrade(super_client, svc, up_svc, primary='1',
                      secondary1='0', secondary2='0')


def test_in_service_upgrade_inactive(context, client, super_client):
    env, svc, up_svc = _insvc_upgrade(context,
                                      client, super_client, True,
                                      activate=False,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      startFirst=True)
    _validate_upgrade(super_client, svc, up_svc, primary='1',
                      secondary1='0', secondary2='0')


def test_in_service_upgrade_all(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}},
                 {'name': "secondary2", 'labels': {'foo': "bar"}}]
    env, svc, up_svc = _insvc_upgrade(context, client,
                                      super_client, True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      secondaryLaunchConfigs=secondary,
                                      batchSize=3,
                                      intervalMillis=100)
    _validate_upgrade(super_client, svc, up_svc,
                      primary='1', secondary1='1', secondary2='1')


def test_in_service_upgrade_one_secondary(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, svc, upgraded_svc = _insvc_upgrade(context, client,
                                            super_client, True,
                                            secondaryLaunchConfigs=secondary,
                                            batchSize=2,
                                            intervalMillis=100)
    _validate_upgrade(super_client, svc, upgraded_svc,
                      primary='0', secondary1='1', secondary2='0')


def test_in_service_upgrade_mix(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, svc, up_svc = _insvc_upgrade(context, client, super_client, True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      secondaryLaunchConfigs=secondary,
                                      batchSize=1)
    _validate_upgrade(super_client, svc, up_svc,
                      primary='1', secondary1='1', secondary2='0')


def test_big_scale(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=10,
                                launchConfig=launch_config,
                                intervalMillis=100)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate(), DEFAULT_TIMEOUT)
    svc = _run_insvc_upgrade(svc,
                             batchSize=1,
                             launchConfig=launch_config)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    svc = client.wait_success(svc.finishupgrade())
    svc = _run_insvc_upgrade(svc,
                             batchSize=5,
                             launchConfig=launch_config)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    client.wait_success(svc.finishupgrade())


def test_rollback_regular_upgrade(context, client, super_client):
    svc, service2, env = _create_env_and_services(context, client,
                                                  4, 4)
    svc = _run_tosvc_upgrade(svc,
                             service2,
                             toServiceId=service2.id,
                             finalScale=4)
    time.sleep(1)
    svc = wait_state(client, svc.cancelupgrade(), 'canceled-upgrade')
    svc = wait_state(client, svc.rollback(), 'active')
    _wait_for_map_count(super_client, svc)


def _create_and_schedule_inservice_upgrade(client, context, startFirst=False):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=4,
                                launchConfig=launch_config,
                                image=image_uuid)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate(), timeout=DEFAULT_TIMEOUT)
    svc = _run_insvc_upgrade(svc, batchSize=2,
                             launchConfig=launch_config,
                             startFirst=startFirst,
                             intervalMillis=100)

    def upgrade_not_null():
        return _validate_in_svc_upgrade(client, svc)

    svc = wait_for(upgrade_not_null, DEFAULT_TIMEOUT)
    return svc


def test_rollback_inservice_upgrade(context, client, super_client):
    svc = _create_and_schedule_inservice_upgrade(client, context)
    time.sleep(1)
    svc = _cancel_upgrade(client, svc)
    _rollback(client, super_client, svc, 1, 0, 0)


def test_cancelupgrade_remove(context, client):
    # upgrading-cancelingupgrade-canceledupgrade-remove
    svc = _create_and_schedule_inservice_upgrade(client, context)
    svc = _cancel_upgrade(client, svc)
    svc.remove()


def test_cancelupgrade_rollback(context, client):
    # upgrading-cancelingupgrade-canceledupgrade-rollback-remove
    svc = _create_and_schedule_inservice_upgrade(client, context)
    svc = _cancel_upgrade(client, svc)
    svc = client.wait_success(svc.rollback())
    svc.remove()


def test_cancelupgrade_finish(context, client):
    # upgrading-cancelingupgrade-canceledupgrade-finishupgrade
    svc = _create_and_schedule_inservice_upgrade(client, context)
    svc = _cancel_upgrade(client, svc)
    svc.continueupgrade()


def test_upgrade_finish_cancel_rollback(context, client):
    # upgrading-cancelingupgrade-canceledupgrade-finishupgrade
    svc = _create_and_schedule_inservice_upgrade(client, context)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'upgraded'
    svc = svc.finishupgrade()
    wait_for(lambda: client.reload(svc).state == 'finishing-upgrade')
    svc = _cancel_upgrade(client, svc)
    assert svc.state == 'canceled-upgrade'
    svc = client.wait_success(svc.rollback())


def test_state_transition_start_first(context, client):
    # upgrading--upgrade-rollback
    # a) startFirst=false
    svc = _create_and_schedule_inservice_upgrade(client, context,
                                                 startFirst=False)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'upgraded'
    svc = svc.rollback()
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'active'
    return
    # b) startFirst=true
    svc = _create_and_schedule_inservice_upgrade(client, context,
                                                 startFirst=True)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'upgraded'
    svc = svc.rollback()
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'active'

    # upgraded->removed
    svc = _create_and_schedule_inservice_upgrade(client, context,
                                                 startFirst=False)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.state == 'upgraded'
    client.wait_success(svc.remove())


def test_in_service_upgrade_networks_from(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, "networkMode": 'container',
                     "networkLaunchConfig": "secondary1"}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=2,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary1],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)
    _validate_upgrade(super_client, svc, u_svc,
                      primary='1', secondary1='1', secondary2='0')


def test_in_service_upgrade_volumes_from(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1",
                  "dataVolumesFromLaunchConfigs": ['secondary2']}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=2,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    u_svc = _run_insvc_upgrade(svc,
                               launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)
    _validate_upgrade(super_client, svc, u_svc,
                      primary='1', secondary1='1', secondary2='1')


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    return env


def test_dns_service_upgrade(client):
    env = _create_stack(client)
    labels = {"foo": "bar"}
    launch_config = {"labels": labels}
    dns = client.create_dnsService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config)
    dns = client.wait_success(dns)
    assert dns.launchConfig is not None
    assert dns.launchConfig.labels == labels
    dns = client.wait_success(dns.activate())

    labels = {"bar": "foo"}
    launch_config = {"labels": labels}
    dns = _run_insvc_upgrade(dns, batchSize=1,
                             launchConfig=launch_config)
    dns = client.wait_success(dns, DEFAULT_TIMEOUT)
    assert dns.launchConfig is not None
    assert dns.launchConfig.labels == labels


def test_external_service_upgrade(client):
    env = _create_stack(client)
    labels = {"foo": "bar"}
    launch_config = {"labels": labels}
    ips = ["72.22.16.5", '192.168.0.10']
    svc = client.create_externalService(name=random_str(),
                                        stackId=env.id,
                                        externalIpAddresses=ips,
                                        launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.launchConfig is not None
    assert svc.launchConfig.labels == labels
    svc = client.wait_success(svc.activate())

    labels = {"bar": "foo"}
    launch_config = {"labels": labels}
    svc = _run_insvc_upgrade(svc, batchSize=1,
                             launchConfig=launch_config)
    svc = client.wait_success(svc, DEFAULT_TIMEOUT)
    assert svc.launchConfig is not None
    assert svc.launchConfig.labels == labels


def test_service_upgrade_no_image_selector(client):
    env = _create_stack(client)
    launch_config = {"imageUuid": "rancher/none"}
    svc1 = client.create_service(name=random_str(),
                                 stackId=env.id,
                                 launchConfig=launch_config,
                                 selectorContainer="foo=barbar")
    svc1 = client.wait_success(svc1)
    svc1 = client.wait_success(svc1.activate())
    strategy = {"intervalMillis": 100, "launchConfig": {}}

    svc1.upgrade_action(launchConfig=launch_config,
                        inServiceStrategy=strategy)


def test_service_upgrade_mixed_selector(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    svc2 = client.create_service(name=random_str(),
                                 stackId=env.id,
                                 launchConfig=launch_config,
                                 selectorContainer="foo=barbar")
    svc2 = client.wait_success(svc2)
    svc2 = client.wait_success(svc2.activate())
    _run_insvc_upgrade(svc2, launchConfig=launch_config)


def test_rollback_sidekicks(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=3,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    initial_maps = super_client. \
        list_serviceExposeMap(serviceId=svc.id,
                              state='active', upgrade=False)

    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary1],
                               batchSize=2)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'

    u_svc = client.wait_success(u_svc.rollback(), DEFAULT_TIMEOUT)
    assert u_svc.state == 'active'
    final_maps = super_client. \
        list_serviceExposeMap(serviceId=u_svc.id,
                              state='active', upgrade=False)

    for initial_map in initial_maps:
        found = False
        for final_map in final_maps:
            if final_map.id == initial_map.id:
                found = True
                break
        assert found is True


def test_upgrade_env(client):
    env = client.create_stack(name='env-' + random_str())
    env = client.wait_success(env)
    assert env.state == 'active'

    env = env.upgrade()
    assert env.state == 'upgrading'

    env = client.wait_success(env)
    assert env.state == 'upgraded'


def test_upgrade_rollback_env(client):
    env = client.create_stack(name='env-' + random_str())
    env = client.wait_success(env)

    assert env.state == 'active'
    assert 'upgrade' in env

    env = env.upgrade()
    assert env.state == 'upgrading'

    env = client.wait_success(env)
    assert env.state == 'upgraded'
    assert 'rollback' in env

    env = env.rollback()
    assert env.state == 'rolling-back'

    env = client.wait_success(env)
    assert env.state == 'active'


def _run_insvc_upgrade(svc, **kw):
    kw["intervalMillis"] = 100
    svc = svc.upgrade_action(inServiceStrategy=kw)
    assert svc.state == 'upgrading'
    return svc


def _insvc_upgrade(context, client, super_client, finish_upgrade,
                   activate=True, **kw):
    env, svc = _create_multi_lc_svc(super_client, client, context, activate)

    _run_insvc_upgrade(svc, **kw)

    def upgrade_not_null():
        return _validate_in_svc_upgrade(client, svc)

    u_svc = wait_for(upgrade_not_null)
    u_svc = client.wait_success(u_svc, timeout=DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    if finish_upgrade:
        u_svc = client.wait_success(u_svc.finishupgrade(),
                                    DEFAULT_TIMEOUT)
        assert u_svc.state == 'active'
    return env, svc, u_svc


def _validate_in_svc_upgrade(client, svc):
    s = client.reload(svc)
    upgrade = s.upgrade
    if upgrade is not None:
        strategy = upgrade.inServiceStrategy
        c1 = strategy.previousLaunchConfig is not None
        c2 = strategy.previousSecondaryLaunchConfigs is not None
        if c1 or c2:
            return s


def _wait_for_instance_start(super_client, id):
    wait_for(
        lambda: len(super_client.by_id('container', id)) > 0
    )
    return super_client.by_id('container', id)


def _wait_for_map_count(super_client, service, launchConfig=None):
    def get_active_launch_config_instances():
        match = []
        instance_maps = super_client. \
            list_serviceExposeMap(serviceId=service.id,
                                  state='active', upgrade=False)
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
        if len(match) == service.scale:
            return match

    wait_for(active_len)
    return get_active_launch_config_instances()


def _validate_upgraded_instances_count(super_client,
                                       svc,
                                       primary=0,
                                       secondary1=0, secondary2=0):
    if primary == 1:
        lc = svc.launchConfig
        _validate_launch_config(super_client, lc, svc)
    if secondary1 == 1:
        lc = svc.secondaryLaunchConfigs[0]
        _validate_launch_config(super_client, lc, svc)
    if secondary2 == 1:
        lc = svc.secondaryLaunchConfigs[1]
        _validate_launch_config(super_client, lc, svc)


def _validate_launch_config(super_client, launchConfig, svc):
    match = _get_upgraded_instances(super_client, launchConfig, svc)
    if len(match) == svc.scale:
        return match


def _get_upgraded_instances(super_client, launchConfig, svc):
    c_name = svc.name
    if hasattr(launchConfig, 'name'):
        c_name = svc.name + "-" + launchConfig.name
    match = []
    instances = super_client. \
        list_container(state='running',
                       accountId=svc.accountId)
    for instance in instances:
        if c_name in \
                instance.name and instance.version == launchConfig.version:
            labels = {'foo': "bar"}
            assert all(item in instance.labels for item in labels) is True
            match.append(instance)
    return match


def _create_env_and_services(context, client, from_scale=1, to_scale=1):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'

    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}

    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    scale=from_scale,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    service = client.wait_success(service.activate(), timeout=DEFAULT_TIMEOUT)
    assert service.state == 'active'
    assert service.upgrade is None

    service2 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     scale=to_scale,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)
    service2 = client.wait_success(service2.activate(),
                                   timeout=DEFAULT_TIMEOUT)
    assert service2.state == 'active'
    assert service2.upgrade is None

    return service, service2, env


def _run_tosvc_upgrade(service, service2, **kw):
    kw['toServiceId'] = service2.id
    service = service.upgrade_action(toServiceStrategy=kw)
    assert service.state == 'upgrading'
    return service


def _run_upgrade(context, client, from_scale, to_scale, **kw):
    service, service2, env = _create_env_and_services(context, client,
                                                      from_scale, to_scale)

    _run_tosvc_upgrade(service, service2, **kw)

    def upgrade_not_null():
        s = client.reload(service)
        if s.upgrade is not None:
            return s

    service = wait_for(upgrade_not_null)

    service = client.wait_success(service, timeout=DEFAULT_TIMEOUT)
    assert service.state == 'upgraded'
    assert service.scale == 0

    service2 = client.wait_success(service2)
    assert service2.state == 'active'
    assert service2.scale == kw['finalScale']

    service = client.wait_success(service.finishupgrade(),
                                  DEFAULT_TIMEOUT)
    assert service.state == 'active'


def _create_multi_lc_svc(super_client, client, context, activate=True):
    env = client.create_stack(name=random_str())
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
                                stackId=env.id,
                                scale=2,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=secondary)
    svc = client.wait_success(svc)

    if activate:
        svc = client.wait_success(svc.activate(), timeout=DEFAULT_TIMEOUT)
        assert svc.state == 'active'
        c11, c11_sec1, c11_sec2, c12, c12_sec1, c12_sec2 \
            = _get_containers(super_client, svc)
        assert svc.launchConfig.version is not None
        assert svc.secondaryLaunchConfigs[0].version is not None
        assert svc.secondaryLaunchConfigs[1].version is not None
        assert c11.version == svc.launchConfig.version
        assert c12.version == svc.launchConfig.version
        assert c11_sec1.version == svc.secondaryLaunchConfigs[0].version
        assert c12_sec1.version == svc.secondaryLaunchConfigs[0].version
        assert c11_sec2.version == svc.secondaryLaunchConfigs[1].version
        assert c12_sec2.version == svc.secondaryLaunchConfigs[1].version

    return env, svc


def _get_containers(super_client, service):
    i_maps = _wait_for_map_count(super_client, service)

    c11 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    i_maps = _wait_for_map_count(super_client, service, 'secondary1')
    c11_sec1 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12_sec1 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    i_maps = _wait_for_map_count(super_client, service, 'secondary2')
    c11_sec2 = _wait_for_instance_start(super_client, i_maps[0].instanceId)
    c12_sec2 = _wait_for_instance_start(super_client, i_maps[1].instanceId)

    return c11, c11_sec1, c11_sec2, c12, c12_sec1, c12_sec2


def _validate_upgrade(super_client, svc, upgraded_svc,
                      primary='0', secondary1='0', secondary2='0'):
    # validate number of upgraded instances first
    _validate_upgraded_instances_count(super_client,
                                       upgraded_svc,
                                       primary, secondary1, secondary2)

    primary_v = svc.launchConfig.version
    sec1_v = svc.secondaryLaunchConfigs[0].version
    sec2_v = svc.secondaryLaunchConfigs[1].version

    primary_upgraded_v = primary_v
    sec1_upgraded_v = sec1_v
    sec2_upgraded_v = sec2_v
    strategy = upgraded_svc.upgrade.inServiceStrategy
    if primary == '1':
        primary_upgraded_v = upgraded_svc.launchConfig.version
        primary_prev_v = strategy.previousLaunchConfig.version
        assert primary_v != primary_upgraded_v
        assert primary_prev_v == primary_v
    if secondary1 == '1':
        sec1_upgraded_v = upgraded_svc.secondaryLaunchConfigs[0].version
        sec1_prev_v = strategy.previousSecondaryLaunchConfigs[0].version
        assert sec1_v != sec1_upgraded_v
        assert sec1_prev_v == sec1_v
    if secondary2 == '1':
        sec2_upgraded_v = upgraded_svc.secondaryLaunchConfigs[1].version
        sec2_prev_v = strategy.previousSecondaryLaunchConfigs[1].version
        assert sec2_v != sec2_upgraded_v
        assert sec2_prev_v == sec2_v

    c21, c21_sec1, c21_sec2, c22, c22_sec1, c22_sec2 = \
        _get_containers(super_client, upgraded_svc)
    assert upgraded_svc.launchConfig.version == primary_upgraded_v
    assert upgraded_svc.secondaryLaunchConfigs[0].version == sec1_upgraded_v
    assert upgraded_svc.secondaryLaunchConfigs[1].version == sec2_upgraded_v
    assert c21.version == upgraded_svc.launchConfig.version
    assert c22.version == upgraded_svc.launchConfig.version
    assert c21_sec1.version == upgraded_svc.secondaryLaunchConfigs[0].version
    assert c22_sec1.version == upgraded_svc.secondaryLaunchConfigs[0].version
    assert c21_sec2.version == upgraded_svc.secondaryLaunchConfigs[1].version
    assert c22_sec2.version == upgraded_svc.secondaryLaunchConfigs[1].version


def _validate_rollback(super_client, svc, rolledback_svc,
                       primary='0', secondary1='0', secondary2='0'):
    # validate number of upgraded instances first
    _validate_upgraded_instances_count(super_client,
                                       svc,
                                       primary, secondary1, secondary2)
    strategy = svc.upgrade.inServiceStrategy
    if primary == 1:
        primary_v = rolledback_svc.launchConfig.version
        primary_prev_v = strategy.previousLaunchConfig.version
        assert primary_prev_v == primary_v
        maps = _wait_for_map_count(super_client, rolledback_svc)
        for map in maps:
            i = _wait_for_instance_start(super_client, map.instanceId)
            assert i.version == primary_v
    if secondary1 == 1:
        sec1_v = rolledback_svc.secondaryLaunchConfigs[0].version
        sec1_prev_v = strategy.previousSecondaryLaunchConfigs[0].version
        assert sec1_prev_v == sec1_v
        maps = _wait_for_map_count(super_client, rolledback_svc, "secondary1")
        for map in maps:
            i = _wait_for_instance_start(super_client, map.instanceId)
            assert i.version == sec1_v
    if secondary2 == 1:
        sec2_v = rolledback_svc.secondaryLaunchConfigs[1].version
        sec2_prev_v = strategy.previousSecondaryLaunchConfigs[1].version
        assert sec2_prev_v == sec2_v
        maps = _wait_for_map_count(super_client, rolledback_svc, "secondary2")
        for map in maps:
            i = _wait_for_instance_start(super_client, map.instanceId)
            assert i.version == sec2_v


def _cancel_upgrade(client, svc):
    svc.cancelupgrade()
    wait_for(lambda: client.reload(svc).state == 'canceled-upgrade')
    svc = client.reload(svc)
    strategy = svc.upgrade.inServiceStrategy
    assert strategy.previousLaunchConfig is not None
    assert strategy.previousSecondaryLaunchConfigs is not None
    return svc


def _rollback(client, super_client,
              svc, primary=0, secondary1=0, secondary2=0):
    rolledback_svc = client.wait_success(svc.rollback(),
                                         DEFAULT_TIMEOUT)
    assert rolledback_svc.state == 'active'
    roll_v = rolledback_svc.launchConfig.version
    strategy = svc.upgrade.inServiceStrategy
    assert roll_v == strategy.previousLaunchConfig.version
    _validate_rollback(super_client, svc, rolledback_svc,
                       primary, secondary1, secondary2)


def test_rollback_id(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                image=image_uuid)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate(), timeout=DEFAULT_TIMEOUT)
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())

    svc = _run_insvc_upgrade(svc, batchSize=2,
                             launchConfig=launch_config,
                             startFirst=False,
                             intervalMillis=100)

    svc = client.wait_success(svc)

    svc = client.wait_success(svc.rollback(), DEFAULT_TIMEOUT)
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c2 = super_client.reload(expose_map.instance())
    assert c1.uuid == c2.uuid


def test_in_service_upgrade_port_mapping(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, 'ports': ['80', '82/tcp']}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1",
                  'ports': ['90']}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'ports': ['100']}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    launch_config = {"imageUuid": image_uuid,
                     'ports': ['80', '82/tcp', '8083:83/udp']}
    u_svc = _run_insvc_upgrade(svc, launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary1,
                                                       secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(),
                                DEFAULT_TIMEOUT)

    svc.launchConfig.ports.append(unicode('8083:83/udp'))
    assert u_svc.launchConfig.ports == svc.launchConfig.ports

    assert u_svc.secondaryLaunchConfigs[0].ports \
        == svc.secondaryLaunchConfigs[0].ports
    assert u_svc.secondaryLaunchConfigs[1].ports \
        == svc.secondaryLaunchConfigs[1].ports


def test_sidekick_addition(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    c2_pre = _validate_compose_instance_start(client, svc, env,
                                              "1", "secondary1")

    u_svc = _run_insvc_upgrade(svc,
                               launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 3, client)
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version != '0'
    c2 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary1")
    assert c2.version == '0'
    assert c2.id == c2_pre.id
    c3 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary2")
    assert c3.version != '0'


def test_sidekick_addition_rollback(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=2,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c11_pre = _validate_compose_instance_start(client, svc, env, "1")
    c12_pre = _validate_compose_instance_start(client, svc, env, "2")
    c21_pre = _validate_compose_instance_start(client, svc, env, "1",
                                               "secondary1")
    c22_pre = _validate_compose_instance_start(client, svc, env, "2",
                                               "secondary1")

    u_svc = _run_insvc_upgrade(svc,
                               launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.rollback(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 4, client)
    c11 = _validate_compose_instance_start(client, svc, env, "1")
    assert c11.version == '0'
    assert c11.id == c11_pre.id
    c12 = _validate_compose_instance_start(client, svc, env, "2")
    assert c12.version == '0'
    assert c12.id == c12_pre.id
    c21 = _validate_compose_instance_start(client, svc, env, "1",
                                           "secondary1")
    assert c21.version == '0'
    assert c21.id == c21_pre.id
    c22 = _validate_compose_instance_start(client, svc, env, "2",
                                           "secondary1")
    assert c22.version == '0'
    assert c22.id == c22_pre.id


def test_sidekick_addition_wo_primary(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")
    c2_pre = _validate_compose_instance_start(client, svc, env, "1",
                                              "secondary1")

    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 3, client)
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary1")
    assert c2.version == '0'
    assert c2.id == c2_pre.id
    c3 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary2")
    assert c3.version != '0'


def test_sidekick_addition_two_sidekicks(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")

    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary1, secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 3, client)
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version != '0'
    c3 = _validate_compose_instance_start(client, svc, env, "1", "secondary2")
    assert c3.version != '0'


def test_sidekick_removal(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")

    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'imageUuid': "rancher/none"}
    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary1, secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.finishupgrade(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 2, client)
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version != '0'


def test_sidekick_removal_rollback(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1"}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary1,
                                                        secondary2])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")
    c2_pre = _validate_compose_instance_start(client, svc, env, "1",
                                              "secondary1")
    c3_pre = _validate_compose_instance_start(client, svc, env, "1",
                                              "secondary2")

    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'imageUuid': "rancher/none"}
    u_svc = _run_insvc_upgrade(svc,
                               secondaryLaunchConfigs=[secondary1, secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc, DEFAULT_TIMEOUT)
    assert u_svc.state == 'upgraded'
    u_svc = client.wait_success(u_svc.rollback(), DEFAULT_TIMEOUT)

    # validate that all service instances are present, and their version
    _wait_until_active_map_count(u_svc, 3, client)
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version == '0'
    assert c2.id == c2_pre.id
    c3 = _validate_compose_instance_start(client, svc, env, "1", "secondary2")
    assert c3.version == '0'
    assert c3.id == c3_pre.id


def _wait_until_active_map_count(service, count, client):
    def wait_for_map_count(service):
        m = client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == count

    wait_for(lambda: wait_for_condition(client, service, wait_for_map_count))
    return client. \
        list_serviceExposeMap(serviceId=service.id, state='active')


def _validate_compose_instance_start(client, service, env,
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


def test_upgrade_global_service(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    host3 = register_simulated_host(new_context)
    client.wait_success(host1)
    client.wait_success(host2)
    client.wait_success(host3)

    # create stack and services
    env = _create_stack(client)
    image_uuid = new_context.image_uuid
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true'
        }
    }
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    c = client. \
        list_serviceExposeMap(serviceId=service.id, state='active')

    strategy = {"launchConfig": launch_config,
                "intervalMillis": 100}
    service.upgrade_action(inServiceStrategy=strategy)
    wait_for(lambda: client.reload(service).state == 'upgraded')

    service = client.reload(service)

    # rollback the service
    service = client.wait_success(service.rollback())
    _wait_until_active_map_count(service, len(c), client)
