from common_fixtures import *  # NOQA


def test_settings_create_delete(admin_user_client, random_str):
    name = random_str + '-test'
    value = random_str + '-value'
    s = admin_user_client.create_setting(name=name, value=value)

    assert s.name == name
    assert s.value == value

    s = admin_user_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == value

    admin_user_client.delete(s)

    s = admin_user_client.by_id_setting(s.id)
    assert s.value is None


def test_settings_update(admin_user_client, random_str):
    name = random_str + '-test'
    value = random_str + '-value'
    s = admin_user_client.create_setting(name=name, value=value)

    assert s.name == name
    assert s.value == value

    s = admin_user_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == value

    s = admin_user_client.update(s, value='new')
    assert s is not None
    assert s.name == name
    assert s.value == 'new'

    s = admin_user_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == 'new'

    s = admin_user_client.update(s, value='new2')
    assert s is not None
    assert s.name == name
    assert s.value == 'new2'

    s = admin_user_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == 'new2'

    admin_user_client.delete(s)

    s = admin_user_client.by_id_setting(s.id)
    assert s.value is None


def test_console_update_existing(admin_user_client):
    id = 'console.agent.port'

    s = admin_user_client.by_id_setting(id)

    assert s.value == '9346'

    s = admin_user_client.update(s, value='9000')
    assert s.value == '9000'

    s = admin_user_client.by_id_setting(id)
    assert s.value == '9000'

    s = admin_user_client.update(s, value='9346')
    assert s.value == '9346'

    s = admin_user_client.by_id_setting(id)
    assert s.value == '9346'


def test_settings_insert(admin_user_client, random_str):
    name = random_str + '-test'
    value = random_str + '-value'
    value2 = random_str + '-value2'

    s = admin_user_client.create_setting(name=name, value=value)
    assert s.name == name
    assert s.value == value
    s = admin_user_client.by_id_setting(s.name)

    s2 = admin_user_client.create_setting(name=name, value=value2)
    assert s2.name == name
    assert s2.value == value2
    assert s.id == s2.id

    admin_user_client.delete(s)
