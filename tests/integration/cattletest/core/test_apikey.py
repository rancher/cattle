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
    assert key.secretValue is None


def test_api_key_create_admin(super_client):
    assert_required_fields(super_client.create_api_key)

    key = super_client.create_api_key()
    assert key.state == 'registering'
    assert key.publicValue is not None
    assert key.secretValue is not None

    key = super_client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue is not None
    assert key.secretValue is not None


def test_api_key_null_secret(super_client, context):
    public_value = random_str()
    key = super_client.create_api_key(accountId=context.project.id,
                                      publicValue=public_value,
                                      secretValue=None)
    assert key.state == 'registering'
    assert key.publicValue == public_value
    assert key.secretValue is None

    key = super_client.wait_transitioning(key)
    assert key.state == 'active'
    assert key.publicValue == public_value
    assert key.secretValue is None


def test_api_key_422_on_identical_keys(admin_user_client):
    public_value = random_str()
    secret_value = random_str()

    key = admin_user_client.create_api_key(publicValue=public_value,
                                           secretValue=secret_value)

    admin_user_client.wait_transitioning(key)
    with pytest.raises(ApiError) as e:
        admin_user_client.create_api_key(publicValue=public_value,
                                         secretValue=secret_value)
    assert e.value.error.status == 422

    public_value = random_str()
    secret_value = random_str()

    key = admin_user_client.create_api_key(publicValue=public_value,
                                           secretValue=secret_value)

    admin_user_client.wait_transitioning(key)
    with pytest.raises(ApiError) as e:
        admin_user_client.create_api_key(publicValue=public_value,
                                         secretValue=secret_value)
    assert e.value.error.status == 422


def test_identical_public_value_password(admin_user_client):
    public_value = random_str()
    secret_value = random_str()
    key = admin_user_client.create_password(publicValue=public_value,
                                            secretValue=secret_value)
    admin_user_client.wait_success(key)
    with pytest.raises(ApiError) as e:
        admin_user_client.create_password(publicValue=public_value,
                                          secretValue=secret_value)
    assert e.value.error.status == 422
    public_value = random_str()
    secret_value = random_str()
    admin_user_client.create_password(publicValue=public_value,
                                      secretValue=secret_value)
    with pytest.raises(ApiError) as e:
        admin_user_client.create_password(publicValue=public_value,
                                          secretValue=secret_value)
    assert e.value.error.status == 422


def test_identical_public_value_password_inactive_removed(admin_user_client):
    public_value = random_str() + random_str()
    secret_value = random_str()
    key = admin_user_client.create_password(publicValue=public_value,
                                            secretValue=secret_value)
    key = admin_user_client.wait_success(key)
    admin_user_client.wait_success(key.deactivate())

    with pytest.raises(ApiError) as e:
        admin_user_client.create_password(publicValue=public_value,
                                          secretValue=secret_value)
    assert e.value.error.status == 422


def test_identical_public_value_api_key_inactive_removed(admin_user_client):
    public_value = random_str() + random_str()
    secret_value = random_str()
    key = admin_user_client.create_api_key(publicValue=public_value,
                                           secretValue=secret_value)
    key = admin_user_client.wait_success(key)
    admin_user_client.wait_success(key.deactivate())

    with pytest.raises(ApiError) as e:
        admin_user_client.create_api_key(publicValue=public_value,
                                         secretValue=secret_value)
    assert e.value.error.status == 422


def test_credential_download(client):
    key = client.create_api_key()
    key = client.wait_success(key)

    blob = key.certificate()
    assert blob is not None
