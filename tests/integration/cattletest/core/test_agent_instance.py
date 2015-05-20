from common_fixtures import *  # NOQA


def test_agent_instance_create(super_client, new_context):
    account_id = new_context.project.id
    network = create_and_activate(super_client, 'network',
                                  accountId=account_id)
    vnet = create_and_activate(super_client, 'vnet',
                               accountId=account_id,
                               networkId=network.id,
                               uri='test:///')
    ni = create_and_activate(super_client, 'agentInstanceProvider',
                             accountId=account_id,
                             networkId=network.id)
    ni2 = create_and_activate(super_client, 'agentInstanceProvider',
                              accountId=account_id,
                              networkId=network.id)
    network_service = create_and_activate(super_client, 'networkService',
                                          accountId=account_id,
                                          networkServiceProviderId=ni.id,
                                          networkId=network.id)
    create_and_activate(super_client, 'networkService',
                        accountId=account_id,
                        networkServiceProviderId=ni.id,
                        networkId=network.id)
    create_and_activate(super_client, 'networkService',
                        accountId=account_id,
                        networkServiceProviderId=ni2.id,
                        networkId=network.id)

    assert network_service.networkServiceProvider().id == ni.id

    c = super_client.create_container(networkMode=None,
                                      imageUuid=new_context.image_uuid,
                                      accountId=account_id,
                                      vnetIds=[vnet.id])
    c = super_client.wait_success(c)
    assert c.state == 'running'

    ni = super_client.reload(ni)
    ni2 = super_client.reload(ni2)

    maps = ni.networkServiceProviderInstanceMaps()
    maps2 = ni2.networkServiceProviderInstanceMaps()

    assert len(maps) == 1
    assert len(maps2) == 1

    agent_instance = super_client.wait_success(maps[0].instance())
    agent_instance2 = super_client.wait_success(maps2[0].instance())

    assert agent_instance.id != agent_instance2.id
    assert agent_instance.accountId == c.accountId
    assert agent_instance.accountId == agent_instance2.accountId

    assert agent_instance.agentId is not None
    assert agent_instance.privileged
    assert agent_instance.instanceTriggeredStop == 'restart'
    assert agent_instance2.agentId is not None
    assert agent_instance.instanceTriggeredStop == 'restart'
    assert agent_instance2.privileged

    items = set([x.name for x in agent_instance.agent().configItemStatuses()])

    assert set(['configscripts',
                'monit',
                'services',
                'agent-instance-startup',
                'agent-instance-scripts']) == items


def test_agent_instance_two_vnet_create(super_client, new_context):
    host = new_context.host
    host2 = register_simulated_host(new_context)
    account_id = new_context.project.id

    network = create_and_activate(super_client, 'hostOnlyNetwork',
                                  accountId=account_id,
                                  dynamicCreateVnet=True)
    subnet = create_and_activate(super_client, 'subnet',
                                 accountId=account_id,
                                 networkAddress='192.168.0.0',
                                 networkId=network.id)
    nsip = create_and_activate(super_client, 'agentInstanceProvider',
                               accountId=account_id,
                               networkId=network.id)
    create_and_activate(super_client, 'networkService',
                        accountId=account_id,
                        networkServiceProviderId=nsip.id,
                        networkId=network.id)

    c = super_client.create_container(networkMode=None,
                                      imageUuid=new_context.image_uuid,
                                      accountId=account_id,
                                      requestedHostId=host.id,
                                      subnetIds=[subnet.id])
    c = super_client.wait_success(c)
    assert c.state == 'running'
    assert c.hosts()[0].id == host.id

    c2 = super_client.create_container(networkMode=None,
                                       imageUuid=new_context.image_uuid,
                                       accountId=account_id,
                                       requestedHostId=host2.id,
                                       subnetIds=[subnet.id])
    c2 = super_client.wait_success(c2)
    assert c2.state == 'running'
    assert c2.hosts()[0].id == host2.id

    maps = nsip.networkServiceProviderInstanceMaps()

    assert len(maps) == 2
    instance1 = maps[0].instance()
    instance2 = maps[1].instance()

    assert instance1.id != instance2.id

    assert instance1.hosts()[0].id != instance2.hosts()[0].id
