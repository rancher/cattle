from common_fixtures import *  # NOQA
import time
from cattle import ApiError


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


def test_container_event_create(client, user_sim_context, user_account):
    # Submitting a 'create' containerEvent should result in a container
    # being created.
    host, user_agent_cli = sim_agent_and_host(user_sim_context)
    external_id = random_str()

    container = create_native_container(client, host, external_id,
                                        user_agent_cli, user_account.id)
    assert container.nativeContainer is True
    assert container.state == 'running'


def test_container_event_stop(client, user_sim_context, user_account):
    # Submitting a 'stop' or 'die' containerEvent should result in a
    # container resource being stopped.
    host, agent_cli = sim_agent_and_host(user_sim_context)
    external_id = random_str()
    user_id = user_account.id

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'stop')

    container = client.wait_success(container)
    assert container.state == 'stopped'

    container = client.wait_success(container.start())
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'die')
    container = client.wait_success(container)
    assert container.state == 'stopped'

    # Sending a stop event on a stopped container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'stop')
    container = client.wait_success(container)
    assert container.state == 'stopped'


def test_container_event_start(client, user_sim_context, user_account):
    # Submitting a 'start' containerEvent should result in a container
    # being started.
    host, agent_cli = sim_agent_and_host(user_sim_context)
    external_id = random_str()
    user_id = user_account.id

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'stop')

    container = client.wait_success(container)
    assert container.state == 'stopped'

    create_event(host, external_id, agent_cli, client, user_id, 'start')
    container = client.wait_success(container)
    assert container.state == 'running'

    # Sending a start event to a running container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'start')
    container = client.wait_success(container)
    assert container.state == 'running'


def test_container_event_destroy(client, user_sim_context, user_account):
    # Submitting a 'destroy' containerEvent should result in a container
    # being removed.
    host, agent_cli = sim_agent_and_host(user_sim_context)
    external_id = random_str()
    user_id = user_account.id

    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id)
    assert container.state == 'running'

    create_event(host, external_id, agent_cli, client, user_id, 'destroy')

    container = client.wait_success(container)
    assert container.state == 'removed'

    # Sending a destroy event to a removed container should have no effect
    create_event(host, external_id, agent_cli, client, user_id, 'destroy')
    container = client.wait_success(container)
    assert container.state == 'removed'


def test_rancher_container_events(client, user_sim_context, user_account):
    # A "normal" container (one created in Rancher) should also respond to
    # non-rancher container events
    host, agent_cli = sim_agent_and_host(user_sim_context)
    uuid = "sim:{}".format(random_num())
    user_id = user_account.id

    container = create_container(client, user_sim_context,
                                 imageUuid=uuid,
                                 name=random_str(),
                                 startOnCreate=False)
    container = client.wait_success(container)
    assert container.state == 'stopped'

    inspect = new_inspect(random_str())
    inspect['Config']['Labels'] = {'io.rancher.container.uuid': container.uuid}

    # pass random external id to prove look up by rancher uuid works
    rand = random_str()
    create_event(host, rand, agent_cli, client, user_id, 'start', inspect)
    container = client.wait_success(container)
    assert container.state == 'running'

    create_event(host, rand, agent_cli, client, user_id, 'stop', inspect)
    container = client.wait_success(container)
    assert container.state == 'stopped'

    # Note that we don't pass inspect on destroy because it wont exist. In this
    # case, we have to pass the container's actual externalId
    ext_id = container.externalId
    create_event(host, ext_id, agent_cli, client, user_id, 'destroy')
    container = client.wait_success(container)
    assert container.state == 'removed'


def test_bad_agent(super_client, user_sim_context):
    # Even though super_client will have permissions to create the container
    # event, additional logic should assert that the creator is a valid agent.
    host = user_sim_context['host']

    with pytest.raises(ApiError) as e:
        super_client.create_container_event(
            reportedHostUuid=host.data.fields['reportedUuid'],
            externalId=random_str(),
            externalFrom='busybox:latest',
            externalTimestamp=int(time.time()),
            externalStatus='create')
    assert e.value.error.code == 'CantVerifyAgent'


def test_bad_host(user_sim_context, new_sim_context):
    # If a host doesn't belong to agent submitting the event, the request
    # should fail.
    host = new_sim_context['host']

    agent_account = user_sim_context['agent'].account()
    user_agent_cli = _client_for_agent(agent_account.credentials()[0])

    with pytest.raises(ApiError) as e:
        user_agent_cli.create_container_event(
            reportedHostUuid=host.data.fields['reportedUuid'],
            externalId=random_str(),
            externalFrom='busybox:latest',
            externalTimestamp=int(time.time()),
            externalStatus='create')
    assert e.value.error.code == 'InvalidReference'


def test_requested_ip_address(super_client, client, user_sim_context,
                              user_account):
    create_agent_instance_nsp(super_client, user_sim_context)
    host, agent_cli = sim_agent_and_host(user_sim_context)
    external_id = random_str()
    user_id = user_account.id
    inspect = new_inspect(external_id)
    inspect['NetworkSettings'] = {'IPAddress': '192.168.0.240'}
    container = create_native_container(client, host, external_id,
                                        agent_cli, user_id, inspect=inspect)
    container = super_client.reload(container)
    assert container['data']['fields']['requestedIpAddress'] == '192.168.0.240'
    assert container.nics()[0].network().kind == 'hostOnlyNetwork'


def sim_agent_and_host(user_sim_context):
    agent_account = user_sim_context['agent'].account()
    user_agent_cli = _client_for_agent(agent_account.credentials()[0])
    host = user_sim_context['host']
    return host, user_agent_cli


def create_native_container(client, host, external_id, user_agent_cli,
                            user_account_id, inspect=None):
    if not inspect:
        inspect = new_inspect(external_id)

    create_event(host, external_id, user_agent_cli, client, user_account_id,
                 'create', inspect)

    def container_wait():
        containers = client.list_container(externalId=external_id)
        if len(containers) and containers[0].state != 'requested':
            return containers[0]
    container = wait_for(container_wait)
    container = client.wait_success(container)
    return container


def create_event(host, external_id, agent_cli, client, user_account_id, status,
                 inspect=None):
    timestamp = int(time.time())
    image = 'sim:busybox:latest'
    event = agent_cli.create_container_event(
        reportedHostUuid=host.data.fields['reportedUuid'],
        externalId=external_id,
        externalFrom=image,
        externalTimestamp=timestamp,
        externalStatus=status,
        dockerInspect=inspect)

    assert event.reportedHostUuid == host.data.fields['reportedUuid']
    assert event.externalId == external_id
    assert event.externalFrom == image
    assert event.externalStatus == status
    assert event.externalTimestamp == timestamp

    def event_wait():
        try:
            created = client.reload(event)
            if created.state == 'created':
                return event
        except ApiError as e:
            if e.error.status != 404:
                raise e

    wait_for(event_wait)
    event = client.reload(event)

    assert host.id == event.hostId
    assert user_account_id == event.accountId
    assert event.state == 'created'

    return event


def new_inspect(rand):
    return {'Name': 'name-%s' % rand, 'Config': {'Image': 'sim:fake/image'}}


def _client_for_agent(credentials):
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=credentials.publicValue,
                           secret_key=credentials.secretValue)
