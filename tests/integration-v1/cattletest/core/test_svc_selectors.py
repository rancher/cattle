from common import *  # NOQA
import yaml
from cattle import ApiError


def _create_service(client, env, image_uuid, service_kind):
    labels = {'foo': "bar"}
    if service_kind == "service":
        launch_config = {"imageUuid": image_uuid, "labels": labels}
        service = client.create_service(name=random_str(),
                                        environmentId=env.id,
                                        launchConfig=launch_config)

    elif service_kind == "dnsService":
        launch_config = {"labels": labels}
        service = client.create_dnsService(name=random_str(),
                                           environmentId=env.id,
                                           launchConfig=launch_config)

    elif service_kind == "externalService":
        launch_config = {"labels": labels}
        service = client.create_externalService(name=random_str(),
                                                environmentId=env.id,
                                                launchConfig=launch_config,
                                                hostname="a.com")

    return labels, service


def test_service_add_instance_selector(new_context):
    client = new_context.client
    context = new_context
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
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    selectorContainer="foo=bar")
    service = client.wait_success(service)
    assert service.selectorContainer == "foo=bar"

    service = client.wait_success(service.activate())
    assert service.state == "active"
    compose_config = env.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.dockerComposeConfig)
    svc = document['services'][service.name]
    assert len(svc['labels']) == 1
    export_labels = {"io.rancher.service.selector.container": "foo=bar"}
    assert svc['labels'] == export_labels

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
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_service_mixed_selector_based_wo_image(client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid

    labels = {'foo': "barbar"}
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container1 = client.wait_success(container1)
    assert container1.state == "running"

    launch_config = {"imageUuid": "sim:rancher/none:latest"}
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
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
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_svc_invalid_selector(client):
    env = _create_stack(client)
    launch_config = {"imageUuid": "rancher/none"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              selectorContainer="foo not in barbar")
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
                                environmentId=env.id,
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
