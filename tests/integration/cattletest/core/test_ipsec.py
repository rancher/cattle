from common_fixtures import *  # NOQA


@pytest.fixture(scope='session')
def ipsec_nsp(internal_test_client, sim_context):
    nsp = create_agent_instance_nsp(internal_test_client, sim_context)

    create_and_activate(internal_test_client, 'ipsecTunnelService',
                        networkId=nsp.networkId,
                        networkServiceProviderId=nsp.id)

    return nsp


def test_ipsec_create(internal_test_client, sim_context):
    nsp = create_agent_instance_nsp(internal_test_client, sim_context)

    assert_required_fields(internal_test_client.create_ipsec_tunnel_service,
                           networkId=nsp.networkId)

    ipsec = create_and_activate(internal_test_client, 'ipsecTunnelService',
                                networkId=nsp.networkId,
                                networkServiceProviderId=nsp.id)

    assert ipsec.state == 'active'
    assert ipsec.networkId == nsp.networkId
    assert ipsec.networkServiceProviderId == nsp.id


def test_ipsec_nic_activate(internal_test_client, sim_context, ipsec_nsp):
    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        networkIds=[ipsec_nsp.networkId],
        startOnCreate=False)
    c = internal_test_client.wait_success(c)
    assert c.state == 'stopped'
    assert len(resource_pool_items(internal_test_client, c,
                                   qualifier='hostPort')) == 0

    c = internal_test_client.wait_success(c.start())
    assert c.state == 'running'

    ai = ipsec_nsp.instances()[0]
    host_id = get_plain_id(internal_test_client, ai.hosts()[0])

    assert ai.state == 'running'
    assert 'nat' in ai.data.ipsec[host_id]
    assert ai.data.ipsec[host_id]['isakmp'] == 500
    assert len(ai.data.fields) > 0

    items = resource_pool_items(internal_test_client, ai, type='instance',
                                qualifier='hostPort')
    assert len(items) == 2

    ai = internal_test_client.wait_success(ai.stop())
    items = resource_pool_items(internal_test_client, ai, type='instance',
                                qualifier='hostPort')
    assert len(items) == 0


def test_ipsec_host_ipaddress_activate(internal_test_client, sim_context,
                                       ipsec_nsp):
    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'],
        networkIds=[ipsec_nsp.networkId])
    c = internal_test_client.wait_success(c)
    assert c.state == 'running'

    ai = ipsec_nsp.instances()[0]
    host = c.hosts()[0]

    item = find_one(internal_test_client.list_config_item_status,
                    agentId=ai.agentId,
                    name='ipsec-hosts')

    ip = internal_test_client.create_ip_address()
    create_and_activate(internal_test_client, 'hostIpAddressMap',
                        hostId=host.id,
                        ipAddressId=ip.id)

    item_after = find_one(internal_test_client.list_config_item_status,
                          agentId=ai.agentId,
                          name='ipsec-hosts')

    assert item.requestedVersion < item_after.requestedVersion
