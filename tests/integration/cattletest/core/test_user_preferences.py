from common_fixtures import *  # NOQA
from gdapi import ApiError


@pytest.fixture(scope='module')
def user_client(context):
    return context.user_client


def _user_preference(client, name=None):
    if name is None:
        name = random_str()
    preference = client.wait_success(client.create_user_preference(
        name=name, value=random_str()))
    got_preference = client.by_id('userPreference', preference.id)
    assert preference.id == got_preference.id
    assert name == got_preference.name
    assert preference.value == got_preference.value
    return got_preference


def test_create_user_preference(user_client):
    _user_preference(user_client)


def test_delete_user_preference(user_client):
    preference = _user_preference(user_client)
    preference = user_client.wait_success(preference.deactivate())
    preference = user_client.wait_success(preference.remove())
    preference = user_client.wait_success(preference.purge())
    preference = user_client.by_id('userPreference', preference.id)
    assert preference.state == 'purged'


def test_update_user_preference(user_client):
    preference = _user_preference(user_client)
    new_value = random_str()
    user_client.update(preference, value=new_value)
    got_preference = user_client.by_id('userPreference', preference.id)
    assert got_preference.value == new_value


def test_unique_user_preference(user_client, admin_user_client):
    rand_str = random_str()
    _user_preference(user_client, rand_str)
    with pytest.raises(ApiError) as e:
        _user_preference(user_client, rand_str)
    assert e.value.error.status == 422
    _user_preference(admin_user_client, rand_str)
    with pytest.raises(ApiError) as e:
        _user_preference(admin_user_client, rand_str)
    assert e.value.error.status == 422
