from common_fixtures import *  # NOQA


def test_u_update_check_revision(client, super_client, context):
    stack = client.create_stack(name=random_str())
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'capAdd': 'AUDIT_CONTROL'}
    svc = client.create_service(name=random_str(),
                                stackId=stack.id,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 1
    rev1 = revisions[0]
    assert rev1.configs[svc.name]['capAdd'] == ['AUDIT_CONTROL']
    assert svc.revisionId == rev1.id
    p_v1 = svc.launchConfig.version
    assert p_v1 == '0'

    # update field that triggers upgrade
    launch_config = {'capAdd': 'AUDIT_WRITE'}
    svc = client.update(svc, launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.previousRevisionId == rev1.id

    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 2
    rev2_id = 0
    for rev in revisions:
        if rev.id != rev1.id:
            assert rev.configs[svc.name]['capAdd'] == ['AUDIT_WRITE']
            rev2_id = rev.id
            break

    assert rev2_id != 0
    assert svc.revisionId == rev2_id
    p_v2 = svc.launchConfig.version
    assert p_v2 != p_v1

    # update ports, validate that the revision is still the same
    launch_config = {'ports': '80:80/tcp'}
    svc = client.update(svc, launchConfig=launch_config)
    svc = client.wait_success(svc)
    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 2
    assert svc.revisionId == rev2_id
    assert svc.launchConfig.ports == ['80:80/tcp']
    p_v3 = svc.launchConfig.version
    assert p_v3 == p_v2

    # add secondary launch config
    launch_config = {'imageUuid': image_uuid, 'name': 'sec'}
    svc = client.update(svc, secondaryLaunchConfigs=[launch_config])
    svc = client.wait_success(svc)
    assert svc.previousRevisionId == rev2_id
    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 3
    rev3_id = 0
    for rev in revisions:
        if rev.id != rev2_id and rev.id != rev1.id:
            assert rev.configs[svc.name]['capAdd'] == ['AUDIT_WRITE']
            rev3_id = rev.id
            break
    assert rev3_id != 0
    assert svc.revisionId == rev3_id
    p_v3 = svc.launchConfig.version
    assert p_v3 == p_v2
    s_v1 = svc.secondaryLaunchConfigs[0].version

    # update secondary launch config upgradable field
    launch_config = {'capAdd': 'AUDIT_WRITE', 'name': 'sec'}
    svc = client.update(svc, secondaryLaunchConfigs=[launch_config])
    svc = client.wait_success(svc)
    assert svc.previousRevisionId == rev3_id
    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 4
    rev4_id = 0
    omit_set = set([rev1.id, rev2_id, rev3_id])
    for rev in revisions:
        if rev.id not in omit_set:
            assert rev.configs[svc.name]['capAdd'] == ['AUDIT_WRITE']
            rev4_id = rev.id
            break
    assert rev4_id != 0
    assert svc.revisionId == rev4_id
    s_v2 = svc.secondaryLaunchConfigs[0].version
    assert s_v1 != s_v2

    # update secondary launch config that doesn't trigger upgrade
    launch_config = {'ports': '80:80/tcp', 'name': 'sec'}
    svc = client.update(svc, secondaryLaunchConfigs=[launch_config])
    svc = client.wait_success(svc)
    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 4
    assert svc.revisionId == rev4_id
    assert svc.secondaryLaunchConfigs[0]['ports'] == ['80:80/tcp']
    s_v3 = svc.secondaryLaunchConfigs[0].version
    assert s_v3 == s_v2

    # remove secondary launchConfig
    launch_config = {'imageUuid': 'rancher/none', 'name': 'sec'}
    svc = client.update(svc, secondaryLaunchConfigs=[launch_config])
    svc = client.wait_success(svc)
    assert svc.previousRevisionId == rev4_id
    revisions = client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 5
    rev5_id = 0
    omit_set = set([rev1.id, rev2_id, rev3_id, rev4_id])
    for rev in revisions:
        if rev.id not in omit_set:
            rev5_id = rev.id
            break
    assert rev5_id != 0
    assert svc.revisionId == rev5_id
    assert svc.secondaryLaunchConfigs == []
    assert super_client.reload(svc).isUpgrade == 0


def test_u_in_service_upgrade_primary(context, client, super_client):
    env, svc, up_svc = _insvc_upgrade(context,
                                      client,
                                      super_client,
                                      True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      startFirst=True)
    _validate_upgrade(super_client, svc, up_svc, primary='1',
                      secondary1='0', secondary2='0')


def test_u_in_service_upgrade_inactive(context, client, super_client):
    env, svc, up_svc = _insvc_upgrade(context,
                                      client, super_client,
                                      False,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      startFirst=True)
    assert up_svc.state == 'inactive'
    up_svc = client.wait_success(svc.activate())
    _validate_upgrade(super_client, svc, up_svc, primary='1',
                      secondary1='0', secondary2='0')


def test_u_in_service_upgrade_all(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}},
                 {'name': "secondary2", 'labels': {'foo': "bar"}}]
    env, svc, up_svc = _insvc_upgrade(context, client,
                                      super_client,
                                      True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      secondaryLaunchConfigs=secondary,
                                      batchSize=3,
                                      intervalMillis=100)
    _validate_upgrade(super_client, svc, up_svc,
                      primary='1', secondary1='1', secondary2='1')


def test_u_in_service_upgrade_one_secondary(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, svc, upgraded_svc = _insvc_upgrade(context, client,
                                            super_client, True,
                                            secondaryLaunchConfigs=secondary,
                                            batchSize=2,
                                            intervalMillis=100)
    _validate_upgrade(super_client, svc, upgraded_svc,
                      primary='0', secondary1='1', secondary2='0')


def test_u_in_service_upgrade_mix(context, client, super_client):
    secondary = [{'name': "secondary1", 'labels': {'foo': "bar"}}]
    env, svc, up_svc = _insvc_upgrade(context, client, super_client, True,
                                      launchConfig={'labels': {'foo': "bar"}},
                                      secondaryLaunchConfigs=secondary,
                                      batchSize=1)
    _validate_upgrade(super_client, svc, up_svc,
                      primary='1', secondary1='1', secondary2='0')


def test_u_big_scale(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    launch_config = {'imageUuid': image_uuid,
                     'networkMode': None,
                     'prePullOnUpgrade': False}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=5,
                                launchConfig=launch_config,
                                intervalMillis=100)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    svc = _run_insvc_upgrade(client, svc,
                             batchSize=1,
                             launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = _run_insvc_upgrade(client, svc,
                             batchSize=5,
                             launchConfig=launch_config)
    client.wait_success(svc)


def test_u_cancelupgrade_rollback(context, client):
    # upgrading-pausing-paused-rollback
    svc = _create_and_schedule_inservice_upgrade(client, context)
    svc = _cancel_upgrade(client, svc)
    client.wait_success(svc.rollback())


def test_u_in_service_upgrade_networks_from(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid, "networkMode": 'container',
           "networkLaunchConfig": "secondary1",
           'prePullOnUpgrade': False}
    s11 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False}
    s12 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=2,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[s11,
                                                        s12])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    p11 = _validate_compose_instance_start(client, svc, env, "1")
    p12 = _validate_compose_instance_start(client, svc, env, "2")

    s11 = _validate_compose_instance_start(client, svc, env,
                                           "1", "secondary1")
    s12 = _validate_compose_instance_start(client, svc, env,
                                           "2", "secondary1")

    s21 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False, 'dns': ['10.1.1.1']}
    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[s21],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'
    _validate_upgrade(super_client, svc, u_svc,
                      primary='0', secondary1='1', secondary2='0')
    # check that primary containers got recreated
    wait_for(lambda: client.reload(p11).state == 'removed')
    wait_for(lambda: client.reload(p12).state == 'removed')
    _validate_compose_instance_start(client, svc, env, "1")
    _validate_compose_instance_start(client, svc, env, "2")
    wait_for(lambda: client.reload(s11).state == 'stopped')
    wait_for(lambda: client.reload(s12).state == 'stopped')


def test_u_in_service_upgrade_volumes_from(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False}
    sec11 = {"imageUuid": image_uuid, "name": "secondary1",
             "dataVolumesFromLaunchConfigs": ['secondary2'],
             'prePullOnUpgrade': False}
    sec12 = {"imageUuid": image_uuid, "name": "secondary2",
             'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=2,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[sec11,
                                                        sec12])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    s11 = _validate_compose_instance_start(client, svc, env,
                                           "1", "secondary1")
    s12 = _validate_compose_instance_start(client, svc, env,
                                           "2", "secondary1")

    s21 = _validate_compose_instance_start(client, svc, env,
                                           "1", "secondary2")
    s22 = _validate_compose_instance_start(client, svc, env,
                                           "2", "secondary2")

    lc2 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    sec22 = {"imageUuid": image_uuid, "name": "secondary2",
             'prePullOnUpgrade': False,
             'dns': ['10.1.1.1']}
    u_svc = _run_insvc_upgrade(client, svc,
                               launchConfig=lc2,
                               secondaryLaunchConfigs=[sec22],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'
    _validate_upgrade(super_client, svc, u_svc,
                      primary='1', secondary1='0', secondary2='1')
    _validate_compose_instance_start(client, svc, env, "1")
    _validate_compose_instance_start(client, svc, env, "2")
    wait_for(lambda: client.reload(s11).state == 'removed')
    wait_for(lambda: client.reload(s12).state == 'removed')
    wait_for(lambda: client.reload(s21).state == 'stopped')
    wait_for(lambda: client.reload(s22).state == 'stopped')


def test_u_dns_service_upgrade(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
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
    launch_config = {"labels": labels,
                     'prePullOnUpgrade': False}
    dns = _run_insvc_upgrade(client, dns, batchSize=1,
                             launchConfig=launch_config)
    dns = client.wait_success(dns)
    assert dns.launchConfig is not None
    assert dns.launchConfig.labels == labels


def test_u_external_service_upgrade(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    labels = {"foo": "bar"}
    launch_config = {"labels": labels,
                     'prePullOnUpgrade': False}
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
    svc = _run_insvc_upgrade(client, svc, batchSize=1,
                             launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.launchConfig is not None
    assert svc.launchConfig.labels == labels


def test_u_service_upgrade_mixed_selector(client, context):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     'prePullOnUpgrade': False}
    svc2 = client.create_service(name=random_str(),
                                 stackId=env.id,
                                 launchConfig=launch_config,
                                 selectorContainer="foo=barbar")
    svc2 = client.wait_success(svc2)
    svc2 = client.wait_success(svc2.activate())
    _run_insvc_upgrade(client, svc2, launchConfig=launch_config)


def test_u_rollback_sidekicks(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False}
    s11 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False}
    s12 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=3,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[s11,
                                                        s12])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    initial_maps = super_client. \
        list_serviceExposeMap(serviceId=svc.id,
                              state='active', upgrade=False)

    s22 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False, 'dns': ['10.1.1.1']}
    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[s22],
                               batchSize=2)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    u_svc = client.wait_success(u_svc.rollback())
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


def test_u_rollback_id(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc1 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=lc1,
                                image=image_uuid)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())

    lc2 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    svc = _run_insvc_upgrade(client, svc, batchSize=2,
                             launchConfig=lc2,
                             startFirst=False,
                             intervalMillis=100)

    svc = client.wait_success(svc)

    svc = client.wait_success(svc.rollback())
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c2 = super_client.reload(expose_map.instance())
    assert c1.uuid == c2.uuid


def test_u_in_service_upgrade_port_mapping(context, client, super_client):
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
                     'ports': ['80', '82/tcp', '8083:83/udp'],
                     'prePullOnUpgrade': False}
    u_svc = _run_insvc_upgrade(client, svc, launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary1,
                                                       secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    svc.launchConfig.ports.append(unicode('8083:83/udp'))
    assert u_svc.launchConfig.ports == svc.launchConfig.ports

    assert u_svc.secondaryLaunchConfigs[0].ports \
        == svc.secondaryLaunchConfigs[0].ports
    assert u_svc.secondaryLaunchConfigs[1].ports \
        == svc.secondaryLaunchConfigs[1].ports


def test_u_sidekick_addition(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False}
    s11 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[s11])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())

    c2_pre = _validate_compose_instance_start(client, svc, env,
                                              "1", "secondary1")

    lc2 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    s12 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False}
    u_svc = _run_insvc_upgrade(client, svc,
                               launchConfig=lc2,
                               secondaryLaunchConfigs=[s12],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    # validate that all service instances are present, and their version
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version != '0'
    c2 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary1")
    assert c2.version == '0'
    assert c2.id == c2_pre.id
    c3 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary2")
    assert c3.version != '0'


def test_u_sidekick_addition_rollback(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     'prePullOnUpgrade': False}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1",
                  'prePullOnUpgrade': False}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'prePullOnUpgrade': False}
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

    u_svc = _run_insvc_upgrade(client, svc,
                               launchConfig=launch_config,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'
    u_svc = client.wait_success(u_svc.rollback())

    # validate that all service instances are present, and their version
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


def test_u_sidekick_addition_wo_primary(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     'prePullOnUpgrade': False}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1",
                  'prePullOnUpgrade': False}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'prePullOnUpgrade': False}
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

    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    # validate that all service instances are present, and their version
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


def test_u_sidekick_addition_two_sidekicks(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     'prePullOnUpgrade': False}
    secondary1 = {"imageUuid": image_uuid, "name": "secondary1",
                  'prePullOnUpgrade': False}
    secondary2 = {"imageUuid": image_uuid, "name": "secondary2",
                  'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")

    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[secondary1, secondary2],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    # validate that all service instances are present, and their version
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version != '0'
    c3 = _validate_compose_instance_start(client, svc, env, "1", "secondary2")
    assert c3.version != '0'


def test_u_sidekick_removal(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False}
    s11 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False}
    s12 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[s11,
                                                        s12])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")

    s21 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    s22 = {"imageUuid": image_uuid, "name": "secondary2",
           'imageUuid': "rancher/none"}
    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[s21, s22],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'

    # validate that all service instances are present, and their version
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version != '0'


def test_u_sidekick_removal_rollback(context, client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = context.image_uuid
    lc1 = {"imageUuid": image_uuid,
           'prePullOnUpgrade': False}
    s11 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False}
    s12 = {"imageUuid": image_uuid, "name": "secondary2",
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=1,
                                launchConfig=lc1,
                                secondaryLaunchConfigs=[s11,
                                                        s12])
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c1_pre = _validate_compose_instance_start(client, svc, env, "1")
    c2_pre = _validate_compose_instance_start(client, svc, env, "1",
                                              "secondary1")
    c3_pre = _validate_compose_instance_start(client, svc, env, "1",
                                              "secondary2")

    s21 = {"imageUuid": image_uuid, "name": "secondary1",
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    s22 = {"imageUuid": image_uuid, "name": "secondary2",
           'imageUuid': "rancher/none"}
    u_svc = _run_insvc_upgrade(client, svc,
                               secondaryLaunchConfigs=[s21, s22],
                               batchSize=1)
    u_svc = client.wait_success(u_svc)
    assert u_svc.state == 'active'
    u_svc = wait_state(client, u_svc.rollback(), "active")

    # validate that all service instances are present, and their version
    c1 = _validate_compose_instance_start(client, svc, env, "1")
    assert c1.version == '0'
    assert c1.id == c1_pre.id
    c2 = _validate_compose_instance_start(client, svc, env, "1", "secondary1")
    assert c2.version == '0'
    assert c2.id == c2_pre.id
    c3 = _validate_compose_instance_start(client, svc, env, "1", "secondary2")
    assert c3.version == '0'
    assert c3.id == c3_pre.id


def test_u_upgrade_global_service(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    host3 = register_simulated_host(new_context)
    client.wait_success(host1)
    client.wait_success(host2)
    client.wait_success(host3)

    # create stack and services
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    image_uuid = new_context.image_uuid
    lc1 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true'
        }
    }
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=lc1)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    r1 = service.revisionId

    lc2 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true'
        },
        "dns": ['10.1.1.1']
    }
    service = client.update(service, launchConfig=lc2)
    service = client.wait_success(service)
    assert service.state == 'active'
    r2 = service.revisionId
    assert r1 != r2

    # rollback the service
    client.wait_success(service.rollback())


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


def _create_and_schedule_inservice_upgrade(client, context, startFirst=False):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc1 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                scale=4,
                                launchConfig=lc1,
                                image=image_uuid)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    lc2 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    svc = _run_insvc_upgrade(client, svc, batchSize=2,
                             launchConfig=lc2,
                             startFirst=startFirst,
                             intervalMillis=100)
    return svc


def _cancel_upgrade(client, svc):
    svc.pause()
    wait_for(lambda: client.reload(svc).state == 'paused')
    return client.reload(svc)


def _insvc_upgrade(context, client, super_client, activate=True, **kw):
    env, svc = _create_multi_lc_svc(client, super_client, context, activate)

    _run_insvc_upgrade(client, svc, **kw)

    u_svc = client.wait_success(svc)
    if activate:
        assert u_svc.state == 'active'
    return env, svc, u_svc


def _run_insvc_upgrade(client, svc, **kw):
    svc = client.update(svc, kw)
    assert svc.state == 'updating-active' \
        or svc.state == 'updating-inactive'
    return svc


def _create_multi_lc_svc(client, super_client, context, activate=True):
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

    svc = client.wait_success(svc.activate())
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

    if not activate:
        svc = client.wait_success(svc.deactivate())
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


def _wait_for_instance_start(client, id):
    wait_for(
        lambda: len(client.by_id('container', id)) > 0
    )
    return client.by_id('container', id)


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
    revisions = super_client.list_serviceRevision(serviceId=svc.id)
    assert len(revisions) == 2

    prev = {}
    for rev in revisions:
        if rev.id == upgraded_svc.revisionId:
            p_rev = super_client.by_id('serviceRevision',
                                       id=upgraded_svc.previousRevisionId)
            assert p_rev is not None
            prev = p_rev.configs
            break

    assert len(prev) > 0

    if primary == '1':
        primary_upgraded_v = upgraded_svc.launchConfig.version
        primary_prev_v = prev[svc.name].version
        assert primary_v != primary_upgraded_v
        assert primary_prev_v == primary_v
    if secondary1 == '1':
        sec1_upgraded_v = upgraded_svc.secondaryLaunchConfigs[0].version
        sec1_prev_v = prev['secondary1'].version
        assert sec1_v != sec1_upgraded_v
        assert sec1_prev_v == sec1_v
    if secondary2 == '1':
        sec2_upgraded_v = upgraded_svc.secondaryLaunchConfigs[1].version
        sec2_prev_v = prev['secondary2'].version
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


def test_u_rollback_to_revision(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc1 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=lc1,
                                image=image_uuid)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())
    r1 = svc.revisionId

    lc2 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.1']}
    # upgrade once
    svc = _run_insvc_upgrade(client, svc, batchSize=2,
                             launchConfig=lc2,
                             startFirst=False,
                             intervalMillis=100)

    svc = client.wait_success(svc)
    r2 = svc.revisionId
    assert r1 != r2
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c2 = super_client.reload(expose_map.instance())
    assert c1.id != c2.id

    # upgrade second time
    lc3 = {'imageUuid': image_uuid,
           'networkMode': None,
           'prePullOnUpgrade': False,
           'dns': ['10.1.1.2']}
    svc = _run_insvc_upgrade(client, svc, batchSize=2,
                             launchConfig=lc3,
                             startFirst=False,
                             intervalMillis=100)

    svc = client.wait_success(svc)
    r3 = svc.revisionId
    assert r3 != r2
    assert svc.previousRevisionId == r2
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c3 = super_client.reload(expose_map.instance())
    assert c3.id != c2.id

    svc = client.wait_success(svc.rollback(revisionId=r1))
    maps = _wait_for_map_count(super_client, svc)
    expose_map = maps[0]
    c4 = super_client.reload(expose_map.instance())
    assert c4.id == c1.id
    assert svc.revisionId == r1
    assert svc.previousRevisionId == r3

    assert client.reload(c2).state == 'stopped'
    assert client.reload(c3).state == 'stopped'

    svc.garbagecollect()
    wait_for(lambda: client.reload(c2).state == 'removed')
    wait_for(lambda: client.reload(c3).state == 'removed')
    assert client.reload(c1).state == 'running'


def test_svc_update_scale(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=lc, )
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c = _validate_compose_instance_start(client, svc, env, "1")

    # update just the scale
    svc = client.update(svc, launchConfig=lc, scale=2)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'


def test_svc_update_fields_object(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html",
                    "port": 200}
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'healthCheck': health_check}
    svc = client.create_service(name='health-' + random_str(),
                                stackId=env.id,
                                launchConfig=lc, )
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c = _validate_compose_instance_start(client, svc, env, "1")

    # update the field to the same value
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'

    # change the value
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index1.html",
                    "port": 200}
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'healthCheck': health_check}
    svc = client.update(svc, launchConfig=lc)
    wait_for(lambda: client.reload(c).state == 'stopped')
    c = _validate_compose_instance_start(client, svc, env, "1")


def test_svc_update_fields_list(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'dns': ['10.1.1.1', '10.1.1.2']}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=lc, )
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c = _validate_compose_instance_start(client, svc, env, "1")

    # update the field to the same value - noop
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'

    # swap the order - noop
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'dns': ['10.1.1.2', '10.1.1.1']}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'

    # remove one of the values - upgrade
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'dns': ['10.1.1.2']}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    wait_for(lambda: client.reload(c).state == 'stopped')
    c = _validate_compose_instance_start(client, svc, env, "1")

    # remove the entire field - upgrade
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'dns': None}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    wait_for(lambda: client.reload(c).state == 'stopped')


def test_svc_update_fields_maps(context, client, super_client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'
    image_uuid = context.image_uuid
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'labels': {'foo': 'bar', 'bar': 'foo'}}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=lc, )
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c = _validate_compose_instance_start(client, svc, env, "1")

    # update the field to the same value - noop
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'

    # swap the order - noop
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'labels': {'bar': 'foo', 'foo': 'bar'}}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    assert client.reload(c).state == 'running'

    # remove one of the values - upgrade
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'labels': {'foo': 'bar'}}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    wait_for(lambda: client.reload(c).state == 'stopped')
    c = _validate_compose_instance_start(client, svc, env, "1")

    # add the value back - upgrade
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'labels': {'foo': 'bar', 'bar': 'foo'}}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    wait_for(lambda: client.reload(c).state == 'stopped')
    c = _validate_compose_instance_start(client, svc, env, "1")

    # remove the entire field - upgrade
    lc = {'imageUuid': image_uuid,
          'prePullOnUpgrade': False,
          'labels': {}}
    svc = client.update(svc, launchConfig=lc)
    svc = client.wait_success(svc)
    wait_for(lambda: client.reload(c).state == 'stopped')
