from common_fixtures import *  # NOQA
from cattle import ApiError


def _create_stack(client):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_create_duplicated_services(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    service_name = random_str()
    service1 = client.create_service(name=service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    client.wait_success(service1)

    with pytest.raises(ApiError) as e:
        client.create_service(name=service_name,
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'

    with pytest.raises(ApiError) as e:
        client.create_externalService(name=service_name,
                                      environmentId=env.id,
                                      launchConfig=launch_config,
                                      externalIpAddresses=["72.22.16.5"])
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'

    with pytest.raises(ApiError) as e:
        client.create_dnsService(name=service_name,
                                 environmentId=env.id)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'

    # try to update the service with duplicated service name
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    with pytest.raises(ApiError) as e:
        client.update(service, name=service_name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'

    # remove the service and try to re-use its name
    client.wait_success(service1.remove())
    client.create_service(name=service_name,
                          environmentId=env.id,
                          launchConfig=launch_config)


def test_external_service_w_hostname(client, context):
    env = _create_stack(client)
    # try to create external service with both hostname externalips
    with pytest.raises(ApiError) as e:
        ips = ["72.22.16.5", '192.168.0.10']
        client.create_externalService(name=random_str(),
                                      environmentId=env.id,
                                      hostname="a.com",
                                      externalIpAddresses=ips)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_circular_refs(client, context):
    env = _create_stack(client)

    # test direct circular ref
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}

    secondary_lc = {"imageUuid": image_uuid, "name": "secondary",
                    "dataVolumesFromLaunchConfigs": ['primary']}

    with pytest.raises(ApiError) as e:
        client.create_service(name="primary",
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'

    # test indirect circular ref
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary1']}

    s_lc1 = {"imageUuid": image_uuid, "name": "secondary1",
             "dataVolumesFromLaunchConfigs": ['secondary2']}

    s_lc2 = {"imageUuid": image_uuid, "name": "secondary2",
             "dataVolumesFromLaunchConfigs": ['primary']}

    with pytest.raises(ApiError) as e:
        client.create_service(name="primary",
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[s_lc1, s_lc2])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'


def test_validate_image(client, context):
    env = _create_stack(client)

    # 1. invalide image in primary config
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": "ubuntu:14:04"}

    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'imageUuid'

    # 2. invalide image in secondary config
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    secondary_lc = {"imageUuid": "ubuntu:14:04", "name": "secondary"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'imageUuid'


def test_validate_port(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid

    # test invalid format
    launch_config = {"imageUuid": image_uuid, "ports": ["4565tcp"]}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'PortWrongFormat'

    launch_config = {"imageUuid": image_uuid, "ports": ["4565/invalidtcp"]}
    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'PortInvalidProtocol'

    # test reserved port
    launch_config = {"imageUuid": image_uuid, "ports": ["42:43"]}
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerService(name=random_str(),
                                          environmentId=env.id,
                                          launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_vip_requested_ip(client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    # vip out of the range
    with pytest.raises(ApiError) as e:
        vip = "169.255.65.30"
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              vip=vip)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


def test_add_svc_to_removed_stack(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    client.create_service(name=random_str(),
                          environmentId=env.id,
                          launchConfig=launch_config)

    client.create_service(name=random_str(),
                          environmentId=env.id,
                          launchConfig=launch_config)

    env.remove()

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidState'
    assert e.value.error.fieldName == 'environment'

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidState'
    assert e.value.error.fieldName == 'environment'


def test_validate_launch_config_name(client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    svc_name = random_str()
    service = client.create_service(name=svc_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config)

    client.wait_success(service)

    launch_config = {"imageUuid": image_uuid}

    secondary_lc = {"imageUuid": image_uuid,
                    "name": svc_name}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'


def test_validate_service_token(client, context, super_client):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    svc_name = random_str()
    service = client.create_service(name=svc_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config)

    client.wait_success(service)

    service = super_client.reload(service)

    assert service.state == "inactive"
    assert service.data.fields.token is not None

    token = service.data.fields.token

    svc_name = random_str()

    client.update(service, name=svc_name)

    client.wait_success(service)

    service = super_client.reload(service)

    assert service.name == svc_name
    assert service.data.fields.token is not None
    assert service.data.fields.token == token
