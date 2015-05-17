from common_fixtures import *  # NOQA
import re


@pytest.mark.parametrize('kind', ['user', 'admin'])
def test_account_create(kind, admin_user_client, random_str):
    account = admin_user_client.create_account(kind=kind,
                                               name=random_str)

    assert account.state == "registering"
    assert account.transitioning == "yes"

    account = wait_success(admin_user_client, account)

    assert account.transitioning == "no"
    assert account.state == "active"

    count = len(admin_user_client.list_account(name=random_str))
    assert count == 1

    creds = account.credentials()

    assert len(creds) == 2
    creds = filter(lambda x: x.kind == 'apiKey', creds)

    assert len(creds) == 1
    assert creds[0].state == "active"
    assert creds[0].kind == "apiKey"
    assert re.match("[A-Z]*", creds[0].publicValue)
    assert len(creds[0].publicValue) == 20
    assert re.match("[a-zA-Z0-9]*", creds[0].secretValue)
    assert len(creds[0].secretValue) == 40


def test_account_external(admin_user_client):
    account = admin_user_client.create_account(externalId='extid',
                                               externalIdType='extType')
    account = admin_user_client.wait_success(account)

    assert account.state == 'active'
    assert account.externalId == 'extid'
    assert account.externalIdType == 'extType'


def test_account_no_key(super_client):
    account = super_client.create_account(kind='admin')
    account = super_client.wait_success(account)
    creds = account.credentials()

    assert len(creds) >= 2

    account = super_client.create_account(kind='unknown')
    account = super_client.wait_success(account)
    creds = account.credentials()

    assert len(creds) == 0


def test_account_new_data(admin_user_client, super_client):
    user = admin_user_client.create_account(kind='user')
    user = admin_user_client.wait_success(user)

    assert user.state == 'active'
    assert user.defaultNetworkId is None
    assert len(user.networks()) == 0

    account = admin_user_client.create_account(kind='project')
    account = admin_user_client.wait_success(account)

    assert account.state == 'active'
    assert account.defaultNetworkId is not None

    networks = super_client.list_network(accountId=account.id)

    by_kind = {}

    for i in range(len(networks)):
        network = super_client.wait_success(networks[i])
        by_kind[networks[i].kind] = network
        assert network.state == 'active'

    assert len(networks) == 5
    assert len(by_kind) == 5

    assert 'dockerHost' in by_kind
    assert 'dockerNone' in by_kind
    assert 'dockerBridge' in by_kind
    assert 'hostOnlyNetwork' in by_kind
    assert 'dockerContainer' in by_kind

    network = by_kind['hostOnlyNetwork']

    assert network.id == account.defaultNetworkId

    subnet = find_one(network.subnets)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '10.42.0.0'
    assert subnet.cidrSize == 16
    assert subnet.gateway == '10.42.0.1'
    assert subnet.startAddress == '10.42.0.2'
    assert subnet.endAddress == '10.42.255.250'

    nsp = find_one(network.networkServiceProviders)
    nsp = super_client.wait_success(nsp)

    assert nsp.state == 'active'
    assert nsp.kind == 'agentInstanceProvider'

    service_by_kind = {}
    for service in nsp.networkServices():
        service = super_client.wait_success(service)
        service_by_kind[service.kind] = service

    assert len(nsp.networkServices()) == 6
    assert len(service_by_kind) == 6
    assert 'dnsService' in service_by_kind
    assert 'linkService' in service_by_kind
    assert 'ipsecTunnelService' in service_by_kind
    assert 'portService' in service_by_kind
    assert 'hostNatGatewayService' in service_by_kind
    assert 'healthCheckService' in service_by_kind