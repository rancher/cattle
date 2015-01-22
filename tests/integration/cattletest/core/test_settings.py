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
    assert s is None
