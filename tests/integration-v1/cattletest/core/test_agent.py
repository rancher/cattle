from common import *  # NOQA
import time


def test_agent_create(super_client):
    uri = "sim://" + str(time.time())

    agent = super_client.create_agent(uri=uri)

    assert agent.state == "registering"
    assert agent.uri == uri
    assert agent.transitioning == "yes"

    agent = super_client.wait_success(agent)

    assert agent.transitioning == "no"
    assert agent.state == "active"

    assert agent.account() is not None

    count = len(agent.account().credentials())
    assert count == 1

    account = agent.account()
    assert account.uuid.startswith("agentAccount")
    assert account.state == "active"
    assert account.kind == "agent"

    creds = filter(lambda x: x.kind == 'agentApiKey', account.credentials())
    assert len(creds) == 1
    assert creds[0].state == "active"
    assert creds[0].publicValue is not None
    assert creds[0].secretValue is not None


def test_agent_create_for_container(new_context, super_client):
    client = new_context.client
    c = new_context.create_container(labels={
        'io.rancher.container.create_agent': 'true'
    })

    c = super_client.reload(c)
    agent = c.agent()
    account_id = get_plain_id(c.account())

    assert agent.state == 'active'
    assert agent.data.agentResourcesAccountId == int(account_id)

    client.delete(c)
    c = client.wait_success(c)

    assert c.state == 'removed'

    agent = super_client.wait_success(super_client.reload(agent))
    assert agent.state == 'removed'


def test_agent_create_for_env_role(new_context, super_client):
    c = new_context.create_container(labels={
        'io.rancher.container.create_agent': 'true',
        'io.rancher.container.agent.role': 'environment'
    })

    c = super_client.reload(c)
    agent = super_client.wait_success(c.agent())

    assert agent.state == 'active'
    cred = agent.account().credentials()[0]
    assert cred.publicValue is not None
    assert cred.secretValue is not None

    agent_client = api_client(cred.publicValue, cred.secretValue)
    assert 'POST' in agent_client.schema.types['container'].collectionMethods


def test_agent_create_for_not_env_role(new_context, super_client):
    c = new_context.create_container(labels={
        'io.rancher.container.create_agent': 'true',
        'io.rancher.container.agent.role': 'user'
    })

    c = super_client.reload(c)
    agent = super_client.wait_success(c.agent())

    assert agent.state == 'active'
    cred = agent.account().credentials()[0]
    assert cred.publicValue is not None
    assert cred.secretValue is not None

    agent_client = api_client(cred.publicValue, cred.secretValue)
    assert 'container' not in agent_client.schema.types
