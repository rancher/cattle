from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def service_client(admin_user_client):
    return create_context(admin_user_client, kind='service').user_client


def test_register_create(client, super_client):
    assert_required_fields(client.create_register,
                           key='abc')

    key = random_str()

    r = client.create_register(key=key)
    assert r.state == 'registering'

    r = client.wait_success(r)
    assert r.state == 'active'
    assert 'key' not in r
    assert 'accessKey' not in r
    assert 'secretKey' not in r

    r = find_one(client.list_register, key=key)
    assert r.state == 'active'
    assert r.key == key
    assert r.accessKey is not None
    assert r.secretKey is not None

    agent = get_by_plain_id(super_client, 'agent',
                            super_client.reload(r).data.agentId)

    raw_account_id = get_plain_id(super_client, r.account())

    agent = super_client.reload(agent)
    assert str(agent.data.agentResourcesAccountId) == raw_account_id

    assert agent is not None
    agent.deactivate()

    assert agent.data.registrationKey == r.key


def test_registration_token_create(client):
    assert_required_fields(client.create_registration_token)

    t = client.create_registration_token()
    assert t.state == 'registering'

    t = client.wait_success(t)
    assert t.state == 'active'

    assert 'publicValue' not in t
    assert 'secretValue' not in t
    assert t.token is not None

    # Test that tokens don't change, need three just in case we get unlucky
    # and cross the rollover boundry time
    tokens = set()
    tokens.add(client.reload(t).token)
    tokens.add(client.reload(t).token)

    assert len(tokens) == 2 or len(tokens) == 1


@pytest.mark.parametrize('kind', ['user', 'admin'])
def test_registration_token_account_create(kind, admin_user_client,
                                           cattle_url):
    account = create_and_activate(admin_user_client, 'account', kind=kind)

    creds = filter(lambda x: x.kind == 'registrationToken',
                   account.credentials())

    assert len(creds) == 1

    cred = admin_user_client.wait_success(creds[0])
    assert cred.state == 'active'
    assert cred.token is not None

    client = cattle.from_env(url=cattle_url,
                             access_key=cred.kind,
                             secret_key=cred.token)

    types = set(client.schema.types.keys())
    assert set(['register', 'schema']) == types

    auth_check(client.schema, 'register', 'crd', {
        'key': 'cr',
        'accessKey': 'r',
        'secretKey': 'r',
    })


def test_registration_token_list(service_client, client):
    # Proves the service_client has access to all tokens
    tokens = service_client.list_registration_token(limit=1000)
    pre_count = len(tokens)
    assert pre_count < 1000, "Too many tokens. " \
                             "Count: [%s]. Clear database." % pre_count
    t = client.create_registration_token()
    t = client.wait_success(t)
    assert t.state == 'active'
    tokens = service_client.list_registration_token(limit=1000)
    post_count = len(tokens)
    assert post_count > pre_count

    token = service_client.by_id_registration_token(t.id)
    assert token is not None
    assert token.image.startswith('rancher/agent:')
    assert token.command.startswith('sudo docker run')

    parts = token.command.split()

    assert token.registrationUrl == parts[-1]


def test_service_create_token(service_client, client, context):
    # Proves the service account can create a token on behalf of another user
    account_id = context.project.id

    token = service_client.create_registration_token(accountId=account_id)
    token = service_client.wait_success(token)
    assert token.state == 'active'
    assert token.accountId == account_id

    # Prove client has access to it
    assert client.reload(token) is not None
