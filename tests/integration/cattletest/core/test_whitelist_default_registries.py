from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(autouse=True)
def update_event_settings(request, admin_user_client):

    def set_to_null():
        settings = admin_user_client.list_setting()
        for s in settings:
            if s.name == 'registry.whitelist':
                setting = s
                print setting
        setting = admin_user_client.update(setting, {'value': ''})
        wait_setting_active(admin_user_client, setting)

    request.addfinalizer(set_to_null)


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


@pytest.mark.nonparallel
def test_container_registry_default(super_client, client, context,
                                    admin_user_client):

    id = 'registry.default'
    default_registry = admin_user_client.by_id_setting(id)
    default_registry = admin_user_client.update(default_registry,
                                                value='')
    wait_setting_active(admin_user_client, default_registry)

    id = 'registry.whitelist'
    whitelist_registries = admin_user_client.by_id_setting(id)

    # case 1: whitelist is set to different values, default set to null
    whitelist_value = 'index.docker.io,sim:rancher,docker.io'
    whitelist_registries = admin_user_client.update(whitelist_registries,
                                                    value=whitelist_value)
    wait_setting_active(admin_user_client, whitelist_registries)

    uuid = "sim:{}".format(random_num())
    # passes, because no default provided so index.docker.io
    # gets prepended and that's whitelisted
    container = client.create_container(imageUuid=uuid,
                                        startOnCreate=False)

    container = client.wait_success(container)

    assert_fields(container, {
        "type": "container",
        "state": "stopped",
        "imageUuid": uuid,
        "firstRunning": None,
    })

    uuid = "sim:localhost.localdomain:5000/ubuntu:{}".format(random_num())
    # will fail, registry localhost.localdomain:5000 not whitelisted
    with pytest.raises(ApiError) as e:
        client.create_container(imageUuid=uuid,
                                startOnCreate=False)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    # case 2: whitelist and default both set to different values
    default_value = 'sim:localhost.localdomain:5000'
    whitelist_value = 'sim:localhost.localdomain:5000,index.docker.io'
    whitelist_value = whitelist_value+',sim:rancher,docker.io'
    default_registry = admin_user_client.update(default_registry,
                                                value=default_value)
    wait_setting_active(admin_user_client, default_registry)
    whitelist_registries = admin_user_client.update(whitelist_registries,
                                                    value=whitelist_value)
    wait_setting_active(admin_user_client, whitelist_registries)

    uuid = "sim:v2"
    # passes, because no default provided so localhost.localdomain:5000
    # gets prepended and that's whitelisted
    container = client.create_container(imageUuid=uuid,
                                        startOnCreate=False)

    container = client.wait_success(container)

    assert_fields(container, {
        "type": "container",
        "state": "stopped",
        "imageUuid": "sim:localhost.localdomain:5000/sim:v2",
        "firstRunning": None,
    })

    uuid = "sim:localhost.localdomain:5001/ubuntu:{}".format(random_num())
    # will fail, registry localhost.localdomain:5001 not whitelisted
    with pytest.raises(ApiError) as e:
        client.create_container(imageUuid=uuid,
                                startOnCreate=False)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    uuid = "sim:localhost.localdomain:5000/ubuntu:{}".format(random_num())
    # passes, since localhost.localdomain:5000/ whitelisted
    container = client.create_container(imageUuid=uuid,
                                        startOnCreate=False)

    container = client.wait_success(container)

    assert_fields(container, {
        "type": "container",
        "state": "stopped",
        "imageUuid": uuid,
        "firstRunning": None,
    })
    default_registry = admin_user_client.update(default_registry,
                                                value='')
    wait_setting_active(admin_user_client, default_registry)


@pytest.mark.nonparallel
def test_registries(super_client, client, context,
                    admin_user_client):
    id = 'registry.default'
    default_registry = admin_user_client.by_id_setting(id)

    id = 'registry.whitelist'
    whitelist_registries = admin_user_client.by_id_setting(id)

    default_value = 'sim:localhost.localdomain:5000'
    whitelist_value = 'sim:localhost.localdomain:5000,index.docker.io'
    whitelist_value = whitelist_value + ',sim:rancher,docker.io'
    default_registry = admin_user_client.update(default_registry,
                                                value=default_value)
    wait_setting_active(admin_user_client, default_registry)

    whitelist_registries = admin_user_client.update(whitelist_registries,
                                                    value=whitelist_value)
    wait_setting_active(admin_user_client, whitelist_registries)

    env = _create_stack(client)

    uuid = "sim:namespace/ubuntu:{}".format(random_num())
    lc_registry = {"imageUuid": uuid}
    with pytest.raises(ApiError) as e:
        service = client.create_service(name=random_str(),
                                        stackId=env.id,
                                        launchConfig=lc_registry,
                                        scale=1)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    uuid = "sim:localhost.localdomain:5000/ubuntu:{}".format(random_num())
    lc_registry = {"imageUuid": uuid}
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=lc_registry,
                                    scale=1)

    assert service.launchConfig.imageUuid == uuid

    uuid1 = "ubuntu:v1"
    lc_registry = {"imageUuid": uuid1}
    lc_registry_sec = {"imageUuid": uuid1, "name": "secondary"}
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=lc_registry,
                                    secondaryLaunchConfigs=[lc_registry_sec],
                                    scale=1)

    uuid2 = 'sim:localhost.localdomain:5000/ubuntu:v1'
    assert service.launchConfig.imageUuid == uuid2
    assert service.secondaryLaunchConfigs[0].imageUuid == uuid2

    uuid1 = "ubuntu:v1"
    lc_registry = {"imageUuid": uuid1}
    launch_config_upgrade = {"imageUuid": "sim:registry/ubuntu:v1"}
    service = client.create_service(name=random_str(),
                                    stackId=env.id,
                                    launchConfig=lc_registry,
                                    scale=1)
    service = wait_state(client, service, 'inactive')
    with pytest.raises(ApiError) as e:
        strategy = {"launchConfig": launch_config_upgrade,
                    "intervalMillis": 100}
        service.upgrade_action(inServiceStrategy=strategy)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    default_registry = admin_user_client.update(default_registry,
                                                value='')
    wait_setting_active(admin_user_client, default_registry)
