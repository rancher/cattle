from common_fixtures import *  # NOQA


def test_api_key_create(client):
    assert_required_fields(client.create_api_key)

    key = client.create_api_key()
    assert key.state == 'registering'
    assert key.publicValue is not None
    assert key.secretValue is not None

    key = client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue is not None
    assert key.secretValue is not None


def test_api_key_create_admin(client):
    assert_required_fields(client.create_api_key)

    key = client.create_api_key()
    assert key.state == 'registering'
    assert key.publicValue is not None
    assert key.secretValue is not None

    key = client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue is not None
    assert key.secretValue is not None


def test_api_key_null_secret(super_client, context):
    key = super_client.create_api_key(accountId=context.project.id,
                                      publicValue='foo',
                                      secretValue=None)
    assert key.state == 'registering'
    assert key.publicValue == 'foo'
    assert key.secretValue is None

    key = super_client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue == 'foo'
    assert key.secretValue is None
