from common_fixtures import *  # NOQA
from test_physical_host import disable_go_machine_service  # NOQA
import time


def test_container_event(admin_client, client, user_sim_context, user_account):
    agent_account = user_sim_context['agent'].account()
    user_agent_cli = _client_for_agent(agent_account.credentials()[0])

    host = user_sim_context['host']
    event = create_event(host, user_agent_cli)

    event = admin_client.wait_success(event)
    assert host.id == event.hostId
    assert user_account.id == event.accountId
    assert event.state == 'created'

    event = client.reload(event)
    assert host.id == event.hostId
    assert user_account.id == event.accountId

    events = client.list_container_event()
    assert len(events), 'User couldn\'t list events'


def create_event(host, cli):
    timestamp = int(time.time())
    status = 'create'
    image = 'busybox:latest'
    external_id = random_str()
    event = cli.create_container_event(
        reportedHostUuid=host.data.fields['reportedUuid'],
        externalId=external_id,
        externalFrom=image,
        externalTimestamp= timestamp,
        externalStatus=status)

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
