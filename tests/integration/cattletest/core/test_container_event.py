from common_fixtures import *  # NOQA
from test_physical_host import disable_go_machine_service  # NOQA


def test_container_event(admin_client, user_sim_context):
    agent_account = user_sim_context['agent'].account()
    user_agent_cli = _client_for_agent(agent_account.credentials()[0])

    host = user_sim_context['host']
    event = create_event(host, user_agent_cli)

    event = admin_client.wait_success(event)
    assert host.id == event.hostId
    assert agent_account.id == event.accountId
    assert event.state == 'created'


def create_event(host, cli):
    external_id = random_str()
    event = cli.create_container_event(reportedHostUuid=host.uuid,
                                       externalId=external_id,
                                       externalFrom='busybox:latest',
                                       externalTimestamp=1426091566,
                                       externalStatus='create')

    assert event.reportedHostUuid == host.uuid
    assert event.externalId == external_id
    assert event.externalFrom == 'busybox:latest'
    assert event.externalStatus == 'create'
    assert event.externalTimestamp == 1426091566
    return event


def _client_for_agent(credentials):
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=credentials.publicValue,
                           secret_key=credentials.secretValue)
