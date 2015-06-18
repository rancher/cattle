from common_fixtures import *  # NOQA
from gdapi import ApiError


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


def test_api_key_409_on_identical_keys(admin_user_client):
    public_value = random_str()
    secret_value = random_str()
    key = admin_user_client.create_api_key(publicValue=public_value,
                                           secretValue=secret_value)
    key2 = admin_user_client.create_api_key(publicValue=public_value,
                                            secretValue=secret_value)
    admin_user_client.wait_transitioning(key)
    admin_user_client.wait_transitioning(key2)
    with pytest.raises(ApiError) as e:
        api_client(public_value, secret_value)
    assert e.value.error.status == 409
