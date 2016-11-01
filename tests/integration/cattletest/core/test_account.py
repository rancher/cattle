from common_fixtures import *  # NOQA


@pytest.mark.parametrize('kind', ['user', 'admin'])
def test_account_create(kind, admin_user_client, random_str):
    account = admin_user_client.create_account(kind=kind,
                                               name=random_str)

    assert account.state == "registering"
    assert account.transitioning == "yes"

    account = admin_user_client.wait_success(account)

    assert account.transitioning == "no"
    assert account.state == "active"

    count = len(admin_user_client.list_account(name=random_str))
    assert count == 1

    creds = account.credentials()

    assert len(creds) == 1
    creds = filter(lambda x: x.kind == 'apiKey', creds)

    assert len(creds) == 0


def test_account_uuid(admin_user_client):
    a = admin_user_client.create_account(uuid=None)
    assert a.uuid is not None

    uuid = random_str()
    a = admin_user_client.create_account(uuid=uuid)
    assert a.uuid == uuid


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

    assert len(creds) >= 1

    account = super_client.create_account(kind='unknown')
    account = super_client.wait_success(account)
    creds = account.credentials()

    assert len(creds) == 0


def test_account_new_data(admin_user_client, super_client):
    user = admin_user_client.create_account(kind='user')
    user = admin_user_client.wait_success(user)

    assert user.state == 'active'
    assert super_client.reload(user).defaultNetworkId is None
    assert len(user.networks()) == 0

    account = admin_user_client.create_account(kind='project')
    account = admin_user_client.wait_success(account)

    assert account.state == 'active'
    assert super_client.reload(account).defaultNetworkId is None

    networks = super_client.list_network(accountId=account.id)

    by_kind = {}

    for i in range(len(networks)):
        network = super_client.wait_success(networks[i])
        by_kind[networks[i].kind] = network
        assert network.state == 'active'

    assert len(networks) == 4
    assert len(by_kind) == 4

    assert 'dockerHost' in by_kind
    assert 'dockerNone' in by_kind
    assert 'dockerBridge' in by_kind
    assert 'dockerContainer' in by_kind


def test_account_context_create(new_context):
    assert new_context.client is not None
    assert new_context.user_client is not None
    assert new_context.project is not None
    assert new_context.account is not None

    assert len(new_context.user_client.list_project()) == 1


def test_account_purge(admin_user_client, super_client, new_context):
    account_id = new_context.project.id
    account = new_context.project
    client = new_context.client
    image_uuid = 'sim:{}'.format(random_num())
    host = new_context.host
    assert host.state == 'active'

    # Create another host
    host2 = register_simulated_host(new_context)
    assert host2.state == 'active'

    # create containers
    c1 = client.create_container(imageUuid=image_uuid,
                                 requestedHostId=host.id)
    c1 = client.wait_success(c1)
    assert c1.state == 'running'

    c2 = client.create_container(imageUuid=image_uuid,
                                 requestedHostId=host.id)
    c2 = client.wait_success(c2)
    assert c2.state == 'running'

    # create stack and services
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service2 = client.create_service(accountId=account_id,
                                     name=random_str(),
                                     stackId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    env.activateservices()
    service1 = client.wait_success(service1, 120)
    service2 = client.wait_success(service2, 120)
    assert service1.state == "active"
    assert service2.state == "active"

    account = super_client.reload(account)
    account = super_client.wait_success(account.deactivate())
    account = super_client.wait_success(account.remove())
    assert account.removed is not None

    account = super_client.wait_success(account.purge())
    assert account.state == 'purged'

    host = super_client.wait_success(host)
    assert host.removed is not None
    assert host.state == 'purged'

    host2 = super_client.wait_success(host2)
    assert host2.removed is not None
    assert host2.state == 'purged'

    c1 = super_client.wait_success(c1)
    assert c1.removed is not None
    assert c1.state == 'removed'

    c2 = super_client.wait_success(c2)
    assert c2.removed is not None
    assert c2.state == 'removed'

    c1 = super_client.wait_success(c1.purge())
    assert c1.state == 'purged'

    volumes = c1.volumes()
    assert len(volumes) == 0

    wait_state(super_client, service1, 'removed')
    wait_state(super_client, service2, 'removed')
    wait_state(super_client, env, 'removed')


def test_user_account_cant_create_account(admin_user_client):
    account = admin_user_client.create_account(name=random_str(),
                                               kind='user')
    account = admin_user_client.wait_success(account)
    api_key = admin_user_client.create_api_key(accountId=account.id)
    admin_user_client.wait_success(api_key)
    client = api_client(api_key.publicValue, api_key.secretValue)

    with pytest.raises(AttributeError) as e:
        client.create_account()
    assert 'create_account' in e.value.message
