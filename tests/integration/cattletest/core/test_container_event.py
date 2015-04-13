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
        if setting.name == 'manage.nonrancher.containers'\
                and setting.value != 'true':
            update_setting('true', setting)

    def revert_settings():
        for s in originals:
            super_client.update(s[0], s[1])

    request.addfinalizer(revert_settings)


def test_container_event_happy_path(admin_client, client, user_sim_context,
                                    user_account):
    # Submitting a valid containerEvent resource with external status of
    # 'create' should result in a container resource being created.
    agent_account = user_sim_context['agent'].account()
    user_agent_cli = _client_for_agent(agent_account.credentials()[0])

    host = user_sim_context['host']
    rand = random_str()
    event = create_event(host, rand, user_agent_cli)

    event = admin_client.wait_success(event)
    assert host.id == event.hostId
    assert user_account.id == event.accountId
    assert event.state == 'created'

    event = client.reload(event)
    assert host.id == event.hostId
    assert user_account.id == event.accountId

    events = client.list_container_event()
    assert len(events), 'User couldn\'t list events'

    containers = client.list_container(name='name-%s' % rand)
    assert len(containers) == 1
    container = client.wait_success(containers[0])
    assert container.nativeContainer is True
    assert container.state == 'running'


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


def create_event(host, rand, cli):
    timestamp = int(time.time())
    status = 'create'
    image = 'busybox:latest'
    external_id = 'id-%s' % rand
    event = cli.create_container_event(
        reportedHostUuid=host.data.fields['reportedUuid'],
        externalId=external_id,
        externalFrom=image,
        externalTimestamp=timestamp,
        externalStatus=status,
        dockerInspect={'Name': 'name-%s' % rand,
                       'Config': {'Image': 'sim:fake/image'}})

    assert event.reportedHostUuid == host.data.fields['reportedUuid']
    assert event.externalId == external_id
    assert event.externalFrom == image
    assert event.externalStatus == status
    assert event.externalTimestamp == timestamp
    return event


def _client_for_agent(credentials):
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=credentials.publicValue,
                           secret_key=credentials.secretValue)
