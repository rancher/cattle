from common_fixtures import *  # NOQA
from cattle import ApiError


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_activate_svc(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    scale_policy = {"min": 2, "max": 4, "increment": 2}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy,
                                scale=6)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    assert svc.scalePolicy is not None
    assert svc.scalePolicy.min == 2
    assert svc.scalePolicy.max == 4
    assert svc.scalePolicy.increment == 2

    client.wait_success(svc.activate())
    wait_for(lambda: super_client.reload(svc).currentScale >= 2)
    wait_for(lambda: client.reload(svc).healthState == 'healthy')


def test_activate_services_fail(super_client, new_context):
    client = new_context.client
    env = _create_stack(client)
    host = super_client.reload(register_simulated_host(new_context))
    wait_for(lambda: super_client.reload(host).state == 'active',
             timeout=5)

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, 'ports': "5419"}
    scale_policy = {"min": 1, "max": 4}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    # as we have only 2 hosts available,
    # service's final scale should be 2
    svc = client.wait_success(svc.activate())
    assert svc.state == "active"
    wait_for(lambda: super_client.reload(svc).currentScale >= 1)


def test_scale_update(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    scale_policy = {"min": 1, "max": 3}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc = client.wait_success(svc.activate())
    assert svc.state == "active"
    wait_for(lambda: super_client.reload(svc).currentScale >= 1)


def test_validate_scale_policy_create(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    scale_policy = {"min": 2, "max": 1, "increment": 2}
    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              stackId=env.id,
                              launchConfig=launch_config,
                              scalePolicy=scale_policy)
    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLimitExceeded'


def test_validate_scale_policy_update(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    scale_policy = {"min": 1, "max": 4, "increment": 2}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    # update with max scale < min scale
    scale_policy = {"max": 3, "min": 5,
                    "increment": 2}
    with pytest.raises(ApiError) as e:
        client.update(svc, scalePolicy=scale_policy)
    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLimitExceeded'


def test_policy_update(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    scale_policy = {"min": 1, "max": 4, "increment": 2}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc.activate()
    svc = client.wait_success(svc, 120)
    wait_for(lambda: super_client.reload(svc).currentScale >= 1)

    # reduce the max
    scale_policy = {"min": 2, "max": 2,
                    "increment": 1}
    client.update(svc, scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    wait_for(lambda: super_client.reload(svc).currentScale <= 2)


def test_service_affinity_rules_w_policy(super_client, new_context):
    client = new_context.client
    env = _create_stack(client)

    image_uuid = new_context.image_uuid
    name = random_str()
    service_name = "service" + name
    scale_policy = {"min": 1, "max": 3, "increment": 1}
    # test anti-affinity
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_ne":
                "io.rancher.stack_service.name=" +
                env.name + '/' + service_name
        }
    }

    svc = client.create_service(name=service_name,
                                stackId=env.id,
                                launchConfig=launch_config,
                                scalePolicy=scale_policy)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc = client.wait_success(svc.activate(), 120)
    assert svc.state == "active"

    assert svc.currentScale == 1

    # add extra host 1
    host = register_simulated_host(new_context)

    # wait for service instances to be 2
    wait_for(lambda: super_client.reload(svc).currentScale >= 2)

    # add extra host 2
    register_simulated_host(new_context)

    # wait for service instances to be 3
    wait_for(lambda: super_client.reload(svc).currentScale >= 3)

    # deactivate and remove host
    host = client.wait_success(host.deactivate())
    client.wait_success(host.remove())
    # make sure that the serivce gets stcuk in activa
    wait_for(lambda: client.reload(svc).state == 'updating-active')
    # shouldn't become active at this point
    try:
        wait_for(lambda: super_client.reload(svc).state == 'active',
                 timeout=15)
    except Exception:
        pass


def _get_instance_for_service(super_client, serviceId):
    instances = []
    instance_service_maps = super_client. \
        list_serviceExposeMap(serviceId=serviceId)
    for mapping in instance_service_maps:
        instances.append(mapping.instance())
    return instances
