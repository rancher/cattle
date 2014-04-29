from common_fixtures import *  # NOQA

_IMAGE_UUID = 'sim:ai'


@pytest.fixture(scope='module')
def ihn_network(admin_client):
    network = create_type_by_uuid(admin_client, 'hostOnlyNetwork',
                                  'test-ipsec-host-nat',
                                  dynamicCreateVnet=True,
                                  isPublic=True)
    create_type_by_uuid(admin_client, 'subnet',
                        'test-ipsec-host-nat',
                        networkId=network.id,
                        networkAddress='192.168.1.0')
    create_type_by_uuid(admin_client, 'ipsecHostNatService',
                        'test-ipsec-host-nat-service',
                        networkId=network.id,
                        agentInstanceImageUuid=_IMAGE_UUID)

    return network


def test_delegate_agent_create(client, admin_client, sim_context,
                               system_account):
    network = create_and_activate(admin_client, 'hostOnlyNetwork',
                                  dynamicCreateVnet=True,
                                  isPublic=True)
    create_and_activate(admin_client, 'subnet',
                        networkId=network.id,
                        networkAddress='192.168.1.0')
    ni = create_and_activate(admin_client, 'agentInstanceProvider',
                             networkId=network.id,
                             agentInstanceImageUuid=_IMAGE_UUID)
    create_and_activate(admin_client, 'networkService',
                        networkServiceProviderId=ni.id,
                        networkId=network.id)

    c = client.create_container(imageUuid=sim_context['imageUuid'],
                                networkIds=[network.id])
    c = client.wait_success(c)
    assert c.state == 'running'

    c = admin_client.reload(c)

    vnets = network.vnets()
    assert len(vnets) == 1

    uri = 'delegate:///?vnetId={}&networkServiceProviderId={}'.format(
        get_plain_id(admin_client, vnets[0]),
        get_plain_id(admin_client, ni))
    agents = admin_client.list_agent(uri=uri)

    assert len(agents) == 1
    agent = admin_client.wait_success(agents[0])

    assert agent.state == 'active'
    assert agent.account().kind == 'agent'
    assert agent.agentGroupId is None

    instances = agents[0].instances()
    assert len(instances) == 1

    instance = instances[0]
    instance = admin_client.wait_success(instance)

    assert instance.state == 'running'
    assert instance.kind == 'container'
    assert instance.accountId == system_account.id
    assert instance.image().uuid == _IMAGE_UUID
    assert len(instance.nics()) == 1
    assert instance.nics()[0].vnetId == vnets[0].id
    assert instance.hosts()[0].id == c.hosts()[0].id
