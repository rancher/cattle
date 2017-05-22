from common_fixtures import *  # NOQA
import yaml
from cattle import ApiError


def _create_service(client, env, image_uuid, service_kind):
    labels = {'foo': "bar"}
    if service_kind == "service":
        launch_config = {"imageUuid": image_uuid, "labels": labels}
        service = client.create_service(name=random_str(),
                                        stackId=env.id,
                                        launchConfig=launch_config)

    elif service_kind == "loadBalancerService":
        launch_config = {"imageUuid": image_uuid, "labels": labels}
        port_rule = {"hostname": "foo.com",
                     "path": "/bar", "sourcePort": 100,
                     "selector": "foo=bar"}
        port_rules = [port_rule]
        lb_config = {"portRules": port_rules}
        service = client.create_loadBalancerService(name=random_str(),
                                                    stackId=env.id,
                                                    launchConfig=launch_config,
                                                    lbConfig=lb_config)

    elif service_kind == "dnsService":
        launch_config = {"labels": labels}
        service = client.create_dnsService(name=random_str(),
                                           stackId=env.id,
                                           launchConfig=launch_config)

    elif service_kind == "externalService":
        launch_config = {"labels": labels}
        service = client.create_externalService(name=random_str(),
                                                stackId=env.id,
                                                launchConfig=launch_config,
                                                hostname="a.com")

    return labels, service


def _validate_service_link(client, context, service_kind):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    # use case #1 - service having selector's label,
    # is present when service with selector is created
    labels, service = _create_service(client, env, image_uuid, service_kind)
    service = client.wait_success(service)
    assert all(item in service.launchConfig.labels.items()
               for item in labels.items())
    launch_config = {"imageUuid": image_uuid}
    if service_kind == 'loadBalancerService':
        service1 = client.create_service(name=random_str(),
                                         stackId=env.id,
                                         launchConfig=launch_config,
                                         selectorLink="foo=bar",
                                         lbConfig={})
    else:
        service1 = client.create_service(name=random_str(),
                                         stackId=env.id,
                                         launchConfig=launch_config,
                                         selectorLink="foo=bar")
    service1 = client.wait_success(service1)
    assert service1.selectorLink == "foo=bar"
    _validate_add_service_link(service1, service, client)
    # use case #2 - service having selector's label,
    # is added after service with selector creation
    labels, service2 = _create_service(client, env, image_uuid, service_kind)
    service2 = client.wait_success(service2)
    assert all(item in service2.launchConfig.labels.items()
               for item in labels.items())
    _validate_add_service_link(service1, service2, client)

    compose_config = env.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.dockerComposeConfig)
    assert len(document['services'][service1.name]['labels']) == 1
    labels = {"io.rancher.service.selector.link": "foo=bar"}
    assert document['services'][service1.name]['labels'] == labels


def test_service_add_service_link_selector(client, context):
    _validate_service_link(client, context, "loadBalancerService")
    _validate_service_link(client, context, "service")
    _validate_service_link(client, context, "dnsService")
    _validate_service_link(client, context, "externalService")


def test_service_add_instance_selector(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid

    # use case #1 - instance having selector's label,
    # is present when service with selector is created
    labels = {'foo': "bar"}
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container1 = client.wait_success(container1)
    assert container1.state == "running"

    launch_config = {"imageUuid": "rancher/none"}
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=launch_config,
                                    selectorContainer="foo=bar")
    service = client.wait_success(service)
    assert service.selectorContainer == "foo=bar"

    service = client.wait_success(service.activate())
    assert service.state == "active"
    compose_config = env.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.dockerComposeConfig)
    assert len(document['services'][service.name]['labels']) == 1
    export_labels = {"io.rancher.service.selector.container": "foo=bar"}
    assert document['services'][service.name]['labels'] == export_labels

    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=service.id)) == 1
    )
    expose_map = container1.serviceExposeMaps()[0]
    assert expose_map.managed == 0

    # use case #2 - instance having selector's label,
    # is added after service with selector creation
    container2 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container2 = client.wait_success(container2)
    assert container2.state == "running"
    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=service.id)) == 2
    )
    expose_map = container2.serviceExposeMaps()[0]
    assert expose_map.managed == 0


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_service_mixed_selector_based_wo_image(client, context, super_client):
    env = _create_stack(client)
    image_uuid = context.image_uuid

    labels = {'foo': "barbar"}
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container1 = client.wait_success(container1)
    assert container1.state == "running"

    launch_config = {"imageUuid": "docker:rancher/none:latest"}
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=launch_config,
                                    selectorContainer="foo=barbar")
    service = client.wait_success(service)
    assert service.selectorContainer == "foo=barbar"

    service = client.wait_success(service.activate())
    assert service.state == "active"

    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=service.id)) == 1
    )

    # add instance having selector label
    labels = {'foo': "barbar"}
    container2 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container2 = client.wait_success(container2)
    assert container2.state == "running"
    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=service.id)) == 2
    )


def test_service_no_image_no_selector(client, context):
    env = _create_stack(client)

    with pytest.raises(ApiError) as e:
        launch_config = {"imageUuid": "rancher/none"}
        client.create_service(name=random_str(),
                              stackId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_svc_invalid_selector(client):
    env = _create_stack(client)
    launch_config = {"imageUuid": "rancher/none"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              stackId=env.id,
                              launchConfig=launch_config,
                              selectorContainer="foo not in barbar")
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidFormat'

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              stackId=env.id,
                              launchConfig=launch_config,
                              selectorContainer="foo notin barbar",
                              selectorLink="foo not in barbar")
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidFormat'


def test_update_instance_selector(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid

    labels = {'foo1': "bar1"}
    c1 = client.create_container(name=random_str(),
                                 imageUuid=image_uuid,
                                 startOnCreate=True,
                                 labels=labels)
    c1 = client.wait_success(c1)
    assert c1.state == "running"

    labels = {'bar1': "foo1"}
    c2 = client.create_container(name=random_str(),
                                 imageUuid=image_uuid,
                                 startOnCreate=True,
                                 labels=labels)
    c2 = client.wait_success(c2)
    assert c2.state == "running"

    launch_config = {"imageUuid": "rancher/none"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                selectorContainer="foo1=bar1")
    svc = client.wait_success(svc)
    assert svc.selectorContainer == "foo1=bar1"

    svc = client.wait_success(svc.activate())

    wait_for(
        lambda: len(client.
                    list_serviceExposeMap(serviceId=svc.id,
                                          state='active')) == 1
    )
    maps = client.list_serviceExposeMap(serviceId=svc.id,
                                        state='active')
    assert maps[0].instanceId == c1.id

    # update selector, validate c1 got de-registered, and c2 registered
    svc = client.update(svc, selectorContainer="bar1=foo1")
    client.wait_success(svc)

    wait_for(
        lambda: len(client.
                    list_serviceExposeMap(serviceId=svc.id,
                                          state='active')) == 1
    )

    maps = client.list_serviceExposeMap(serviceId=svc.id, state='active')
    assert maps[0].instanceId == c2.id


def test_update_link_selector(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    labels = {'foo1': "bar1"}
    launch_config = {"imageUuid": image_uuid, "labels": labels}
    s1 = client.create_service(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config)
    s1 = client.wait_success(s1)
    assert s1.launchConfig.labels == labels

    labels = {'bar1': "foo1"}
    launch_config = {"imageUuid": image_uuid, "labels": labels}
    s2 = client.create_service(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config)
    s2 = client.wait_success(s2)
    assert s2.launchConfig.labels == labels

    launch_config = {"imageUuid": image_uuid}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                selectorLink="foo1=bar1")
    svc = client.wait_success(svc)
    assert svc.selectorLink == "foo1=bar1"
    _validate_add_service_link(svc, s1, client)

    # update selector link
    service = client.update(svc, selectorLink="bar1=foo1")
    client.wait_success(service)
    _validate_add_service_link(svc, s2, client)
    _validate_remove_service_link(svc, s1, client)


def _validate_add_service_link(s, c, client, timeout=None):
    exp1 = len(client.list_serviceConsumeMap(serviceId=s.id,
                                             consumedServiceId=c.id)) == 1
    exp2 = len(client.list_serviceConsumeMap(serviceId=s.id,
                                             consumedServiceId=c.id)) == 1
    if timeout:
        wait_for(lambda: exp1, timeout)
    else:
        wait_for(lambda: exp2)
    service_maps = client. \
        list_serviceConsumeMap(serviceId=s.id,
                               consumedServiceId=c.id)

    service_map = service_maps[0]
    wait_for_condition(
        client, service_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_service_link(service,
                                  consumedService, client):
    def check():
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id)

        return len(service_maps) == 0

    wait_for(check)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def _validate_svc_instance_map_count(client, service,
                                     state, count, timeout=30):
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state=state)
    while len(instance_service_map) < count:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id, state=state)
        if time.time() - start > timeout:
            raise Exception('Timeout waiting for map to be in correct state')

    return instance_service_map


def _wait_for_instance_start(super_client, id):
    wait_for(
        lambda: len(super_client.by_id('container', id)) > 0
    )
    return super_client.by_id('container', id)


def test_cross_account_selector(admin_user_client, super_client, context):
    a2 = admin_user_client.create_account(kind='project')
    a2 = admin_user_client.wait_success(a2)
    a3 = admin_user_client.create_account(kind='project')
    a3 = admin_user_client.wait_success(a3)
    a1 = admin_user_client.create_project(projectLinks=[a2.id])
    a1 = admin_user_client.wait_success(a1)
    assert a1.projectLinks is not None
    a1 = admin_user_client.wait_success(a1)

    image_uuid = context.image_uuid

    env1 = super_client.create_stack(name=random_str(), accountId=a1.id)
    env1 = super_client.wait_success(env1)
    launch_config = {"imageUuid": image_uuid}
    svc1 = super_client.create_service(name=random_str(),
                                       stackId=env1.id,
                                       launchConfig=launch_config,
                                       selectorLink="foo=bar",
                                       accountId=a1.id)

    svc1 = super_client.wait_success(svc1)

    env2 = super_client.create_stack(name=random_str(), accountId=a2.id)
    env2 = super_client.wait_success(env2)
    labels = {'foo': "bar"}
    launch_config = {"imageUuid": image_uuid, "labels": labels}
    svc2 = super_client.create_service(name=random_str(),
                                       stackId=env2.id,
                                       launchConfig=launch_config,
                                       accountId=a2.id)

    svc2 = super_client.wait_success(svc2)
    assert svc1.accountId != svc2.accountId

    _validate_add_service_link(svc1, svc2, super_client)

    env3 = super_client.create_stack(name=random_str(), accountId=a3.id)
    env3 = super_client.wait_success(env3)
    labels = {'foo': "bar"}
    launch_config = {"imageUuid": image_uuid, "labels": labels}
    svc3 = super_client.create_service(name=random_str(),
                                       stackId=env3.id,
                                       launchConfig=launch_config,
                                       accountId=a3.id)
    svc3 = super_client.wait_success(svc3)
    assert svc3.accountId != svc2.accountId

    try:
        _validate_add_service_link(svc1, svc3, super_client, timeout=5)
    except Exception:
        pass

    # reconcile service links on account links updates
    admin_user_client.update(a1, projectLinks=[a3.id])
    _validate_remove_service_link(svc1, svc2, super_client)
    _validate_add_service_link(svc1, svc3, super_client)
