from common_fixtures import *  # NOQA


def test_agent_instance_create(internal_test_client, sim_context):
    network = create_and_activate(internal_test_client, 'network')
    vnet = create_and_activate(internal_test_client, 'vnet',
                               networkId=network.id,
                               uri='test:///')
    ni = create_and_activate(internal_test_client, 'agentInstanceProvider',
                             networkId=network.id,
                             agentInstanceImageUuid=sim_context['imageUuid'])
    ni2 = create_and_activate(internal_test_client, 'agentInstanceProvider',
                              networkId=network.id,
                              agentInstanceImageUuid=sim_context['imageUuid'])
    network_service = create_and_activate(internal_test_client,
                                          'networkService',
                                          networkServiceProviderId=ni.id,
                                          networkId=network.id)
    create_and_activate(internal_test_client, 'networkService',
                        networkServiceProviderId=ni.id,
                        networkId=network.id)
    create_and_activate(internal_test_client, 'networkService',
                        networkServiceProviderId=ni2.id,
                        networkId=network.id)

    assert network_service.networkServiceProvider().id == ni.id

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], vnetIds=[vnet.id])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'

    ni = internal_test_client.reload(ni)
    ni2 = internal_test_client.reload(ni2)

    maps = ni.networkServiceProviderInstanceMaps()
    maps2 = ni2.networkServiceProviderInstanceMaps()

    assert len(maps) == 1
    assert len(maps2) == 1

    agent_instance = internal_test_client.wait_success(maps[0].instance())
    agent_instance2 = internal_test_client.wait_success(maps2[0].instance())

    assert agent_instance.id != agent_instance2.id

    assert agent_instance.agentId is not None
    assert agent_instance.privileged
    assert agent_instance.instanceTriggeredStop == 'restart'
    assert agent_instance2.agentId is not None
    assert agent_instance.instanceTriggeredStop == 'restart'
    assert agent_instance2.privileged

    items = set([x.name for x in agent_instance.agent().configItemStatuses()])

    assert set(['configscripts',
                'monit',
                'node-services',
                'services',
                'agent-instance-startup',
                'agent-instance-scripts']) == items


def test_agent_instance_two_vnet_create(internal_test_client, sim_context,
                                        sim_context2):
    network = create_and_activate(internal_test_client, 'hostOnlyNetwork',
                                  dynamicCreateVnet=True)
    subnet = create_and_activate(internal_test_client, 'subnet',
                                 networkAddress='192.168.0.0',
                                 networkId=network.id)
    nsip = create_and_activate(internal_test_client, 'agentInstanceProvider',
                               networkId=network.id,
                               agentInstanceImageUuid=sim_context['imageUuid'])
    create_and_activate(internal_test_client, 'networkService',
                        networkServiceProviderId=nsip.id,
                        networkId=network.id)

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        requestedHostId=sim_context['host'].id, subnetIds=[subnet.id])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'
    assert c.hosts()[0].id == sim_context['host'].id

    c2 = internal_test_client.create_container(
        imageUuid=sim_context2['imageUuid'],
        requestedHostId=sim_context2['host'].id, subnetIds=[subnet.id])
    c2 = internal_test_client.wait_success(c2)
    assert c2.state == 'running'
    assert c2.hosts()[0].id == sim_context2['host'].id

    maps = nsip.networkServiceProviderInstanceMaps()

    assert len(maps) == 2
    instance1 = maps[0].instance()
    instance2 = maps[1].instance()

    assert instance1.id != instance2.id

    assert instance1.hosts()[0].id != instance2.hosts()[0].id
