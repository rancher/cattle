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


def make_prefs(client):
    pref_ids = []
    for x in range(0, 5):
        pref_ids.append(
            _user_preference(client, name=random_str()).id)
    return set(pref_ids)


def get_prefs_ids(client, all=False):
    pref_ids = []
    for pref in client.list_user_preference(all=all):
        pref_ids.append(pref.id)
    return set(pref_ids)


def test_create_user_preference(user_client):
    _user_preference(user_client)


def test_delete_user_preference(user_client):
    preference = _user_preference(user_client)
    preference = user_client.wait_success(preference.deactivate())
    preference = user_client.wait_success(preference.remove())
    preference = user_client.by_id('userPreference', preference.id)
    assert preference.state == 'removed'
    preference = _user_preference(user_client)
    preference = user_client.wait_success(preference.remove())
    assert preference.state == 'removed'


def test_update_user_preference(user_client):
    preference = _user_preference(user_client)
    new_value = random_str()
    user_client.update(preference, value=new_value)
    got_preference = user_client.by_id('userPreference', preference.id)
    assert got_preference.value == new_value


def test_update_user_preference_pass_name(user_client):
    preference = _user_preference(user_client)
    new_value = random_str()
    user_client.update(preference, name=preference.name, value=new_value)
    got_preference = user_client.by_id('userPreference', preference.id)
    assert got_preference.value == new_value


def test_unique_user_preference(user_client, new_context):
    rand_str = random_str()
    _user_preference(user_client, name=rand_str)
    with pytest.raises(ApiError) as e:
        _user_preference(user_client, name=rand_str)
    assert e.value.error.status == 422
    _user_preference(new_context.user_client, name=rand_str)
    with pytest.raises(ApiError) as e:
        _user_preference(new_context.user_client, name=rand_str)
    assert e.value.error.status == 422


def test_all_filter_user_preference(admin_user_client, request):
    ctx1 = new_context(admin_user_client, request)
    ctx2 = new_context(admin_user_client, request)
    ctx1_prefs = make_prefs(ctx1.user_client)
    ctx2_prefs = make_prefs(ctx2.user_client)
    got_ctx1_prefs = get_prefs_ids(ctx1.user_client)
    got_ctx2_prefs = get_prefs_ids(ctx2.user_client)
    assert len(ctx1_prefs & got_ctx1_prefs) == len(ctx1_prefs)
    assert len(ctx2_prefs & got_ctx2_prefs) == len(ctx2_prefs)
    assert len(got_ctx1_prefs & got_ctx2_prefs) == 0
    admin_prefs = get_prefs_ids(admin_user_client)
    all_prefs = get_prefs_ids(admin_user_client, all=True)
    assert len(admin_prefs) != len(all_prefs)
    assert admin_prefs <= all_prefs
    assert ctx1_prefs | ctx2_prefs <= all_prefs
    assert len((ctx1_prefs | ctx2_prefs) & admin_prefs) == 0
