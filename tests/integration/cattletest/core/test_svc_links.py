from common_fixtures import *  # NOQA
from cattle import ApiError


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_service_add_remove_service_link(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link service2 to service1
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # remove service link
    service1 = service1.removeservicelink(serviceLink=service_link)
    _validate_remove_service_link(service1, service2, client)

    # validate adding link with the name
    service_link = {"serviceId": service2.id, "name": 'myLink'}
    service1 = service1.addservicelink(serviceLink=service_link)
    service_maps = client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id, name='mylink')
    assert len(service_maps) == 1


def test_links_after_service_remove(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    service1 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link servic2 to service1
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # link service1 to service2
    service_link = {"serviceId": service1.id}
    service2 = service2.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service2, service1, client)

    # remove service1
    service1 = client.wait_success(service1.remove())

    _validate_remove_service_link(service1, service2, client)

    _validate_remove_service_link(service2, service1, client)


def test_link_services_from_diff_env(client, context):
    env1 = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    env2 = _create_stack(client)
    service2 = client.create_service(name=random_str(),
                                     stackId=env2.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # try to link - should work
    service_link = {"serviceId": service2.id}
    service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)


def test_set_service_links(client, context):
    env1 = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    service3 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service3 = client.wait_success(service3)

    # set service2, service3 links for service1
    service_link1 = {"serviceId": service2.id, "name": "link1"}
    service_link2 = {"serviceId": service3.id, "name": "link2"}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_add_service_link(service1, service2, client, "link1")
    _validate_add_service_link(service1, service3, client, "link2")

    # update the link with new name
    service_link1 = {"serviceId": service2.id, "name": "link3"}
    service_link2 = {"serviceId": service3.id, "name": "link4"}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_remove_service_link(service1, service2, client, "link1")
    _validate_remove_service_link(service1, service3, client, "link2")
    _validate_add_service_link(service1, service2, client, "link3")
    _validate_add_service_link(service1, service3, client, "link4")

    # set service2 links for service1
    service_link = {"serviceId": service2.id}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link])
    _validate_remove_service_link(service1, service3, client, "link4")

    # set empty service link set
    service1 = service1.setservicelinks(serviceLinks=[])
    _validate_remove_service_link(service1, service2, client, "link3")

    # try to link to the service from diff stack - should work
    env2 = _create_stack(client)

    service4 = client.create_service(name=random_str(),
                                     stackId=env2.id,
                                     launchConfig=launch_config)
    service4 = client.wait_success(service4)

    service_link = {"serviceId": service4.id}
    service1.setservicelinks(serviceLinks=[service_link])

    env1.remove()
    env2.remove()


def test_link_service_twice(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link service2 to service1
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'serviceId'


def test_dns_service(client, context):
    env = _create_stack(client)
    # create 1 app service, 1 dns service and 2 web services
    # app service would link to dns, and dns to the web services
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    web1 = client.create_service(name=random_str(),
                                 stackId=env.id,
                                 launchConfig=launch_config)
    web1 = client.wait_success(web1)

    web2 = client.create_service(name=random_str(),
                                 stackId=env.id,
                                 launchConfig=launch_config)
    web2 = client.wait_success(web2)

    app = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config)
    app = client.wait_success(app)

    dns = client.create_dnsService(name='tata',
                                   stackId=env.id)
    dns = client.wait_success(dns)

    env.activateservices()
    web1 = client.wait_success(web1, 120)
    web2 = client.wait_success(web2)
    app = client.wait_success(app)
    dns = client.wait_success(dns)
    assert web1.state == 'active'
    assert web2.state == 'active'
    assert app.state == 'active'
    assert dns.state == 'active'

    service_link = {"serviceId": web1.id}
    dns = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(dns, web1, client)

    service_link = {"serviceId": web2.id}
    dns = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(dns, web2, client)

    service_link = {"serviceId": dns.id}
    app = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(app, dns, client)


def test_service_link_emu_docker_link(super_client, client, context):
    env = _create_stack(client)

    dns = client.create_dns_service(name='dns', stackId=env.id)

    server = client.create_service(name='server', launchConfig={
        'imageUuid': context.image_uuid
    }, stackId=env.id)

    server2 = client.create_service(name='server2', launchConfig={
        'imageUuid': context.image_uuid
    }, stackId=env.id)

    service = client.create_service(name='client', launchConfig={
        'imageUuid': context.image_uuid
    }, stackId=env.id)

    server3 = client.create_service(name='server3', launchConfig={
        'imageUuid': context.image_uuid
    }, stackId=env.id)

    server4 = client.create_service(name='server4', launchConfig={
        'imageUuid': context.image_uuid
    }, stackId=env.id)

    service_link1 = {"serviceId": dns.id, "name": "dns"}
    service_link2 = {"serviceId": server.id, "name": "other"}
    service_link3 = {"serviceId": server2.id, "name": "server2"}
    service_link4 = {"serviceId": server3.id}
    service_link5 = {"serviceId": server4.id, "name": ""}
    service. \
        setservicelinks(serviceLinks=[service_link1,
                                      service_link2, service_link3,
                                      service_link4, service_link5])

    dns = client.wait_success(dns)
    assert dns.state == 'inactive'

    server = client.wait_success(server)
    assert server.state == 'inactive'

    server2 = client.wait_success(server2)
    assert server2.state == 'inactive'

    service = client.wait_success(service)
    assert service.state == 'inactive'

    server3 = client.wait_success(server3)
    assert server3.state == 'inactive'

    server4 = client.wait_success(server4)
    assert server4.state == 'inactive'

    dns = client.wait_success(dns.activate())
    assert dns.state == 'active'

    server = client.wait_success(server.activate())
    assert server.state == 'active'

    server2 = client.wait_success(server2.activate())
    assert server2.state == 'active'

    server3 = client.wait_success(server3.activate())
    assert server3.state == 'active'

    server4 = client.wait_success(server4.activate())
    assert server4.state == 'active'

    service = client.wait_success(service.activate())
    assert service.state == 'active'

    instance = find_one(service.instances)
    instance = super_client.reload(instance)

    links = instance.instanceLinks()

    assert len(links) == 4

    for link in links:
        map = link.serviceConsumeMap()
        assert map.consumedServiceId in {server.id, server2.id,
                                         server3.id, server4.id}
        assert link.instanceId is not None
        expose_map = link.targetInstance().serviceExposeMaps()[0]
        if map.consumedServiceId == server.id:
            assert link.linkName == 'other'
            assert expose_map.serviceId == server.id
            assert expose_map.managed == 1
        elif map.consumedServiceId == server2.id:
            assert link.linkName == 'server2'
            assert expose_map.serviceId == server2.id
        elif map.consumedServiceId == server3.id:
            assert link.linkName == 'server3'
            assert expose_map.serviceId == server3.id
        elif map.consumedServiceId == server4.id:
            assert link.linkName == 'server4'
            assert expose_map.serviceId == server4.id


def test_set_service_links_duplicated_service(client, context):
    env1 = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env1.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # set service links having same service id, diff name
    service_link1 = {"serviceId": service2.id, "name": "link1"}
    service_link2 = {"serviceId": service2.id, "name": "link2"}

    service1 = service1. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_add_service_link(service1, service2, client, "link1")
    _validate_add_service_link(service1, service2, client, "link2")

    with pytest.raises(ApiError) as e:
        service1. \
            setservicelinks(serviceLinks=[service_link1, service_link1])
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'


def _validate_add_service_link(service,
                               consumedService, client, link_name=None):
    if link_name is None:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id)
    else:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id,
                                   name=link_name)

    assert len(service_maps) == 1
    if link_name is not None:
        assert service_maps[0].name is not None

    service_map = service_maps[0]
    wait_for_condition(
        client, service_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_service_link(service,
                                  consumedService, client, link_name=None):
    def check():
        if link_name is None:
            service_maps = client. \
                list_serviceConsumeMap(serviceId=service.id,
                                       consumedServiceId=consumedService.id)
        else:
            service_maps = client. \
                list_serviceConsumeMap(serviceId=service.id,
                                       consumedServiceId=consumedService.id,
                                       name=link_name)

        return len(service_maps) == 0

    wait_for(check)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def test_validate_svc_link_name(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # single invalid char
    # cannot contain special chars other than ".", "-", "_", "/"
    service_link = {"serviceId": service2.id, "name": '+'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # multiple invalid chars
    # cannot contain special chars other than ".", "-", "_", "/"
    service_link = {"serviceId": service2.id, "name": '$&()#@'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot start with -
    service_link = {"serviceId": service2.id, "name": '-myLink'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot end with -
    service_link = {"serviceId": service2.id, "name": 'myLink-'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot contain --
    service_link = {"serviceId": service2.id, "name": 'my--Link'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot start with .
    service_link = {"serviceId": service2.id, "name": '.myLink'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot end with .
    service_link = {"serviceId": service2.id, "name": 'myLink.'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # cannot contain ..
    service_link = {"serviceId": service2.id, "name": 'myL..ink'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidCharacters'

    # link with no dots longer that 63
    service_link = {"serviceId": service2.id, "name":
                    'myLinkTOOLONGtoolongtoolongtoo'
                    'longmyLinkTOOLONGtoolongtoolongtoo'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLengthExceeded'

    # link with a . with single part longer that 63
    service_link = {"serviceId": service2.id, "name":
                    'myLinkTOOLONGtoolongtoolongtoo'
                    'longmyLinkTOOLONGtoolongtoolongtoo.secondpart'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLengthExceeded'

    # link with . with total length longer that 253
    service_link = {"serviceId": service2.id, "name":
                    'myLinkTOOLONGtoolongtoolongtoo.'
                    'longmyLinkTOOLONGtoolongtoolongtoo.secondpart.'
                    'myLinkTOOLONGtoolongtoolongtoo.'
                    'longmyLinkTOOLONGtoolongtoolongtoo.secondpart.'
                    'myLinkTOOLONGtoolongtoolongtoo.'
                    'longmyLinkTOOLONGtoolongtoolongtoo.secondpart.'
                    'myLinkTOOLONGtoolongtoolongtoo.'
                    'longmyLinkTOOLONGtoolongtoolongtoo.secondpart'}

    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLengthExceeded'

    # link service2 to service1 with single valid char link
    service_link = {"serviceId": service2.id, "name": 'm'}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    service3 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service3 = client.wait_success(service3)

    # link service3 to service1 with multiple valid chars
    service_link2 = {"serviceId": service3.id, "name": 'm.gh_kl.a-b'}
    service1 = service1.addservicelink(serviceLink=service_link2)
    _validate_add_service_link(service1, service3, client)
