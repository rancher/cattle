from common_fixtures import *  # NOQA

_IMAGE_UUID = 'sim:ai'


def test_delegate_agent_create(super_client, new_context):
    c = new_context.create_container()

    c = super_client.reload(c)

    network = c.nics()[0].network()
    vnets = network.vnets()
    assert len(vnets) == 1

    nsps = super_client.\
        list_network_service_provider(accountId=new_context.project.id)

    assert len(nsps) == 1

    uri = 'delegate:///?vnetId={}&networkServiceProviderId={}'.format(
        get_plain_id(super_client, vnets[0]),
        get_plain_id(super_client, nsps[0]))
    agents = super_client.list_agent(uri=uri)

    assert len(agents) == 1
    agent = super_client.wait_success(agents[0])

    assert agent.state == 'active'
    assert agent.account().kind == 'agent'
    assert agent.agentGroupId is None

    instances = agents[0].instances()
    assert len(instances) == 1

    instance = instances[0]
    instance = super_client.wait_success(instance)

    assert instance.state == 'running'
    assert instance.kind == 'container'
    assert instance.accountId == new_context.project.id
    assert len(instance.nics()) == 1
    assert instance.nics()[0].vnetId == vnets[0].id
    assert instance.hosts()[0].physicalHost().id == \
        c.hosts()[0].physicalHost().id
