from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def ipsec_context(admin_user_client, request):
    return new_context(admin_user_client, request)


def create_agent_instance_nsp(admin_client, account):
    network = create_and_activate(admin_client, 'hostOnlyNetwork',
                                  isPublic=True,
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True,
                                  accountId=account.id)

    create_and_activate(admin_client, 'subnet',
                        networkAddress='192.168.0.0',
                        networkId=network.id,
                        accountId=account.id)

    return create_and_activate(admin_client, 'agentInstanceProvider',
                               networkId=network.id,
                               accountId=account.id)


@pytest.fixture(scope='module')
def ipsec_nsp(super_client, ipsec_context):
    nsp = create_agent_instance_nsp(super_client, ipsec_context.project)

    create_and_activate(super_client, 'ipsecTunnelService',
                        networkId=nsp.networkId,
                        networkServiceProviderId=nsp.id)

    return nsp


def test_ipsec_create(super_client, ipsec_context):
    nsp = create_agent_instance_nsp(super_client, ipsec_context.project)

    assert_required_fields(super_client.create_ipsec_tunnel_service,
                           networkId=nsp.networkId)

    ipsec = create_and_activate(super_client, 'ipsecTunnelService',
                                networkId=nsp.networkId,
                                networkServiceProviderId=nsp.id)

    assert ipsec.state == 'active'
    assert ipsec.networkId == nsp.networkId
    assert ipsec.networkServiceProviderId == nsp.id


def test_ipsec_nic_activate(super_client, ipsec_context, ipsec_nsp):
    c = super_client.create_container(accountId=ipsec_context.project.id,
                                      networkMode=None,
                                      imageUuid=ipsec_context.image_uuid,
                                      networkIds=[ipsec_nsp.networkId],
                                      startOnCreate=False)
    c = super_client.wait_success(c)
    assert c.state == 'stopped'
    assert len(resource_pool_items(super_client, c, qualifier='hostPort')) == 0

    c = super_client.wait_success(c.start())
    assert c.state == 'running'

    ai = ipsec_nsp.instances()[0]
    host_id = get_plain_id(super_client, ai.hosts()[0])

    assert ai.state == 'running'
    assert 'nat' in ai.data.ipsec[host_id]
    assert ai.data.ipsec[host_id]['isakmp'] == 500
    assert len(ai.data.fields) > 0

    items = resource_pool_items(super_client, ai, type='instance',
                                qualifier='hostPort')
    assert len(items) == 2

    ai = super_client.wait_success(ai.stop())
    items = resource_pool_items(super_client, ai, type='instance',
                                qualifier='hostPort')
    assert len(items) == 0


def test_ipsec_host_ipaddress_activate(super_client, ipsec_context, ipsec_nsp):
    c = super_client.create_container(accountId=ipsec_context.project.id,
                                      networkMode=None,
                                      imageUuid=ipsec_context.image_uuid,
                                      networkIds=[ipsec_nsp.networkId])
    c = super_client.wait_success(c)
    assert c.state == 'running'

    ai = ipsec_nsp.instances()[0]
    host = c.hosts()[0]

    item = find_one(super_client.list_config_item_status,
                    agentId=ai.agentId,
                    name='ipsec-hosts')

    ip = super_client.create_ip_address()
    create_and_activate(super_client, 'hostIpAddressMap',
                        hostId=host.id,
                        ipAddressId=ip.id)

    item_after = find_one(super_client.list_config_item_status,
                          agentId=ai.agentId,
                          name='ipsec-hosts')

    assert item.requestedVersion < item_after.requestedVersion
