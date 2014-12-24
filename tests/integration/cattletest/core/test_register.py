from common_fixtures import *  # NOQA


def test_register_create(admin_client, internal_test_client):
    assert_required_fields(admin_client.create_register,
                           key='abc')

    key = random_str()

    r = admin_client.create_register(key=key)
    assert r.state == 'registering'

    r = admin_client.wait_success(r)
    assert r.state == 'active'
    assert 'key' not in r
    assert 'accessKey' not in r
    assert 'secretKey' not in r

    r = find_one(admin_client.list_register, key=key)
    assert r.state == 'active'
    assert r.key == key
    assert r.accessKey is not None
    assert r.secretKey is not None

    agent = get_by_plain_id(admin_client, 'agent', r.data.agentId)

    raw_account_id = get_plain_id(admin_client, r.account())
    agent = internal_test_client.reload(agent)
    assert str(agent.data.agentResourcesAccountId) == raw_account_id

    assert agent is not None
    agent.deactivate()

    assert agent.data.registrationKey == r.key


def test_registration_token_create(admin_client):
    assert_required_fields(admin_client.create_registration_token)

    t = admin_client.create_registration_token()
    assert t.state == 'registering'

    t = admin_client.wait_success(t)
    assert t.state == 'active'

    assert 'publicValue' not in t
    assert 'secretValue' not in t
    assert t.token is not None

    # Test that tokens don't change, need three just in case we get unlucky
    # and cross the rollover boundry time
    tokens = set()
    tokens.add(admin_client.reload(t).token)
    tokens.add(admin_client.reload(t).token)

    assert len(tokens) == 2 or len(tokens) == 1


def test_registration_token_account_create(admin_client, cattle_url):
    account = create_and_activate(admin_client, 'account')

    creds = filter(lambda x: x.kind == 'registrationToken',
                   account.credentials())

    assert len(creds) == 1

    cred = admin_client.wait_success(creds[0])
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
