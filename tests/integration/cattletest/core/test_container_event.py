from common_fixtures import *  # NOQA
import time
from cattle import ApiError
from test_volume import VOLUME_CLEANUP_LABEL


@pytest.fixture(scope='module', autouse=True)
def update_event_settings(request, super_client):
    settings = super_client.list_setting()
    originals = []

    def update_setting(new_value, s):
        originals.append((setting, {'value': s.value}))
        s = super_client.update(s, {'value': new_value})
        wait_setting_active(super_client, s)

    for setting in settings:
        if setting.name == 'manage.nonrancher.containers' \
                and setting.value != 'true':
            update_setting('true', setting)

    def revert_settings():
        for s in originals:
            super_client.update(s[0], s[1])

    request.addfinalizer(revert_settings)


@pytest.fixture(scope='module')
def host(super_client, context):
    return super_client.reload(context.host)


@pytest.fixture(scope='module')
def agent_cli(context):
    return context.agent_client


@pytest.fixture(scope='module')
def user_id(context):
    return context.project.id


def test_container_event_create(client, host, agent_cli, user_id):
    # Submitting a 'start' containerEvent should result in a container
    # being created.
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.nativeContainer is True
    assert container.state == 'running'


def test_volume_cleanup_strategy_label(client, host, agent_cli, user_id):
    external_id = random_str()
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert VOLUME_CLEANUP_LABEL not in container.labels

    external_id = random_str()
    inspect = {'Config': {'Labels': {VOLUME_CLEANUP_LABEL: 'all'}}}
    container = \
        create_native_container(client, host, external_id, agent_cli, user_id,
                                inspect=inspect)
    assert container.labels[VOLUME_CLEANUP_LABEL] == 'all'


def test_container_event_start_stop(client, host, agent_cli, user_id):
    # Submitting a 'stop' or 'die' containerEvent should result in a
    # container resource being stopped.
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'stop')
    container = client.wait_success(container)
    wait_state(client, container, 'stopped')

    create_event(host, external_id, agent_cli, client, user_id, 'start')
    container = client.wait_success(container)
    wait_state(client, container, 'running')

    # Sending a start event on a running container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'start')
    container = client.wait_success(container)
    wait_state(client, container, 'running')

    create_event(host, external_id, agent_cli, client, user_id, 'die')
    container = client.wait_success(container)
    wait_state(client, container, 'stopped')

    # Sending a stop event on a stopped container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'stop')
    container = client.wait_success(container)
    wait_state(client, container, 'stopped')


def test_container_event_start(client, host, agent_cli, user_id):
    # Submitting a 'start' containerEvent should result in a container
    # being started.
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'stop')

    containers = client.list_container(externalId=external_id)
    assert len(containers) == 1
    container = client.wait_success(containers[0])
    wait_state(client, container, 'stopped')


def test_container_event_remove_start(client, host, agent_cli, user_id):
    # If a container is removed and then an event comes in for the container,
    # the event should be ignored.
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    container = client.wait_success(container.stop())
    wait_state(client, container, 'stopped')
    container = client.wait_success(container.remove())
    wait_state(client, container, 'removed')

    create_event(host, external_id, agent_cli, client, user_id, 'start')
    container = client.wait_success(container)
    wait_state(client, container, 'removed')

    containers = client.list_container(externalId=external_id)
    assert len(containers) == 0


def test_container_event_destroy(client, host, agent_cli, user_id):
    # Submitting a 'destroy' containerEvent should result in a container
    # being removed.
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'destroy')

    container = client.wait_success(container)
    wait_state(client, container, 'removed')

    # Sending a destroy event to a removed container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'destroy')
    container = client.wait_success(container)
    wait_state(client, container, 'removed')


def test_rancher_container_events(client, context, host, agent_cli, user_id):
    # A "normal" container (one created in Rancher) should also respond to
    # non-rancher container events
    container = context.create_container(name=random_str(),
                                         startOnCreate=False)
    assert container.state == 'stopped'

    inspect = new_inspect(random_str())
    inspect['Config']['Labels'] = {'io.rancher.container.uuid': container.uuid}

    # pass random external id to prove look up by rancher uuid works
    rand = random_str()
    create_event(host, rand, agent_cli, client, user_id, 'start', inspect)
    container = client.wait_success(container)
    wait_state(client, container, 'running')

    create_event(host, rand, agent_cli, client, user_id, 'stop', inspect)
    container = client.wait_success(container)
    wait_state(client, container, 'stopped')

    # Note that we don't pass inspect on destroy because it wont exist. In this
    # case, we have to pass the container's actual externalId
    ext_id = container.externalId
    create_event(host, ext_id, agent_cli, client, user_id, 'destroy')
    container = client.wait_success(container)
    wait_state(client, container, 'removed')


def test_bad_agent(super_client, new_context):
    host, account, agent_client = register_simulated_host(new_context,
                                                          return_agent=True)
    host = super_client.reload(host)

    def post():
        agent_client.create_container_event(
            reportedHostUuid=host.externalId,
            externalId=random_str(),
            externalStatus='start')

    # Test it works
    post()

    # Test it fails with two agents
    super_client.wait_success(super_client.create_agent(
        uri='test://' + random_str(),
        accountId=account.id))
    with pytest.raises(ApiError) as e:
        post()
    assert e.value.error.code == 'MissingRequired'

    # Test it fails with no agents
    for agent in super_client.list_agent(accountId=account.id):
        super_client.wait_success(agent.deactivate())
    with pytest.raises(ApiError) as e:
        post()
    assert e.value.error.code == 'CantVerifyAgent'


def test_bad_host(host, new_context):
    # If a host doesn't belong to agent submitting the event, the request
    # should fail.
    agent_cli = new_context.agent_client

    with pytest.raises(ApiError) as e:
        agent_cli.create_container_event(
            reportedHostUuid=host.externalId,
            externalId=random_str(),
            externalStatus='start')
    assert e.value.error.code == 'InvalidReference'


def test_requested_ip_address(super_client, client, host, agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['NetworkSettings'] = {'IPAddress': '10.42.0.240'}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    container = super_client.reload(container)
    assert container['data']['fields']['requestedIpAddress'] == '10.42.0.240'
    network = client.by_id_network(container.primaryNetworkId)
    assert network.kind == 'dockerBridge'
    assert container.primaryIpAddress is None


def test_requested_ip_address_with_managed(super_client, client, host,
                                           agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['NetworkSettings'] = {'IPAddress': '10.42.0.240'}
    inspect['Config'] = {
        'Labels': {
            'io.rancher.container.network': 'true'
        }
    }
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    container = super_client.reload(container)
    assert container['data']['fields']['requestedIpAddress'] == '10.42.0.240'
    assert client.by_id_network(container.primaryNetworkId).kind == 'network'
    assert container.primaryIpAddress == '10.42.0.240'


def test_container_event_net_none(client, host, agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['Config']['NetworkDisabled'] = True
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    assert container['networkMode'] == 'none'


def test_container_event_net_host(client, host, agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['HostConfig'] = {'NetworkMode': 'host'}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    assert container['networkMode'] == 'host'


def test_container_event_net_bridge(client, host, agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['HostConfig'] = {'NetworkMode': 'bridge'}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    assert container['networkMode'] == 'bridge'


def test_container_event_net_blank(client, host, agent_cli, user_id):
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['HostConfig'] = {'NetworkMode': ''}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    assert container['networkMode'] == 'bridge'


def test_container_event_net_container(client, host, agent_cli, user_id):
    target_external_id = random_str()
    target = create_native_container(client, host, target_external_id,
                                     agent_cli, user_id)
    external_id = random_str()
    inspect = new_inspect(external_id)
    inspect['HostConfig'] = {'NetworkMode': 'container:%s' % target.externalId}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    assert container['networkMode'] == 'container'
    assert container['networkContainerId'] == target.id


def test_container_event_image_and_reg_cred(client, host, agent_cli, user_id,
                                            super_client):
    server = 'server{0}.io'.format(random_num())
    registry = client.create_registry(serverAddress=server,
                                      name=random_str())
    registry = client.wait_success(registry)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        publicValue='rancher',
        secretValue='rancher')
    registry_credential = client.wait_success(reg_cred)
    name = server + '/rancher/authorized:latest'
    external_id = random_str()
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id,
                                        inspect=new_inspect(random_str(),
                                                            name))
    assert container.nativeContainer is True
    assert container.state == 'running'
    container = super_client.wait_success(container)
    assert container.registryCredentialId == registry_credential.id


def create_native_container(client, host, external_id, user_agent_cli,
                            user_account_id, inspect=None):
    if not inspect:
        inspect = new_inspect(external_id)

    create_event(host, external_id, user_agent_cli, client, user_account_id,
                 'start', inspect)

    def container_wait():
        containers = client.list_container(externalId=external_id)
        if len(containers) and containers[0].state != 'requested':
            return containers[0]

    container = wait_for(container_wait)
    container = client.wait_success(container)
    return container


def create_event(host, external_id, agent_cli, client, user_account_id, status,
                 inspect=None, wait_and_assert=True):
    event = agent_cli.create_container_event(
        reportedHostUuid=host.externalId,
        externalId=external_id,
        externalStatus=status,
        dockerInspect=inspect)

    assert event.reportedHostUuid == host.externalId
    assert event.externalId == external_id
    assert event.externalStatus == status
    assert host.id == event.hostId

    return event


def new_inspect(rand, image='fake/image'):
    return {'Name': 'name-%s' % rand, 'Config': {'Image': image}}


def _client_for_agent(credentials):
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=credentials.publicValue,
                           secret_key=credentials.secretValue)
