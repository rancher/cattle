from common_fixtures import *  # NOQA


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

    service = wait_for(upgrade_not_null, timeout=2)

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
