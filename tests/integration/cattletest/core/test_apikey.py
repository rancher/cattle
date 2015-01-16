from common_fixtures import *  # NOQA


def test_api_key_create(admin_client):
    assert_required_fields(admin_client.create_api_key)

    key = admin_client.create_api_key()
    assert key.state == 'registering'

    key = admin_client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue is not None
    assert key.secretValue is not None


def test_api_key_create_user(client):
    assert_required_fields(client.create_api_key)

    key = client.create_api_key()
    assert key.state == 'registering'

    key = client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue is not None
