from common_fixtures import *  # NOQA
import time


# sim_context is included to ensure that the simulator context creates
# the first agent and thus the first external simulator pool
def test_agent_create(super_client, sim_context):

    uri = "sim://" + str(time.time())

    agent = super_client.create_agent(uri=uri)

    assert agent.state == "registering"
    assert agent.uri == uri
    assert agent.transitioning == "yes"

    agent = wait_success(super_client, agent)

    assert agent.transitioning == "no"
    assert agent.state == "active"

    assert agent.account() is not None

    count = len(agent.account().credentials())
    assert count == 2

    account = agent.account()
    assert account.uuid.startswith("agentAccount")
    assert account.state == "active"
    assert account.kind == "agent"

    creds = filter(lambda x: x.kind == 'agentApiKey', account.credentials())
    assert len(creds) == 1
    assert creds[0].state == "active"
    assert creds[0].publicValue is not None
    assert creds[0].secretValue is not None
