from common_fixtures import *  # NOQA


def test_settings_create_delete(admin_client, random_str):
    name = random_str + '-test'
    value = random_str + '-value'
    s = admin_client.create_setting(name=name, value=value)

    assert s.name == name
    assert s.value == value

    s = admin_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == value

    admin_client.delete(s)

    s = admin_client.by_id_setting(s.id)
    assert s.value is None


def test_settings_update(admin_client, random_str):
    name = random_str + '-test'
    value = random_str + '-value'
    s = admin_client.create_setting(name=name, value=value)

    assert s.name == name
    assert s.value == value

    s = admin_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == value

    s = admin_client.update(s, value='new')
    assert s is not None
    assert s.name == name
    assert s.value == 'new'

    s = admin_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == 'new'

    s = admin_client.update(s, value='new2')
    assert s is not None
    assert s.name == name
    assert s.value == 'new2'

    s = admin_client.by_id_setting(s.id)
    assert s is not None
    assert s.name == name
    assert s.value == 'new2'

    admin_client.delete(s)

    s = admin_client.by_id_setting(s.id)
    assert s.value is None


def test_console_update_existing(admin_client):
    id = 'console.agent.port'

    s = admin_client.by_id_setting(id)

    assert s.value == '9346'

    s = admin_client.update(s, value='9000')
    assert s.value == '9000'

    s = admin_client.by_id_setting(id)
    assert s.value == '9000'

    s = admin_client.update(s, value='9346')
    assert s.value == '9346'

    s = admin_client.by_id_setting(id)
    assert s.value == '9346'
