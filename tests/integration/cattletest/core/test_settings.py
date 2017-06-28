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
    wait_for(lambda: admin_user_client.by_id_setting(s.id).value is None)


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
    wait_for(lambda: admin_user_client.by_id_setting(s.id).value is None)


def test_exec_update_existing(admin_user_client):
    id = 'exec.agent.path'

    s = admin_user_client.by_id_setting(id)

    assert s.value == '/v1/exec/'

    s = admin_user_client.update(s, value='/v1/exec/2')
    assert s.value == '/v1/exec/2'

    s = admin_user_client.by_id_setting(id)
    assert s.value == '/v1/exec/2'

    s = admin_user_client.update(s, value='/v1/exec/')
    assert s.value == '/v1/exec/'

    s = admin_user_client.by_id_setting(id)
    assert s.value == '/v1/exec/'


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


def test_settings_user_list(context):
    user_client = context.user_client
    settings = user_client.list_setting()
    names = {x.name for x in settings}

    assert len(settings) != 0
    assert len(settings) < 16
    assert 'rancher.compose.linux.url' in names

    settings = user_client.list_setting(all=True)
    assert len(settings) != 0
    assert len(settings) < 16

    settings = user_client.list_setting(all=False)
    assert len(settings) != 0
    assert len(settings) < 16


def test_settings_admin_user_list(admin_user_client):
    settings = admin_user_client.list_setting()
    assert len(settings) != 0
    assert len(settings) > 16

    settings = admin_user_client.list_setting(all=True)
    assert len(settings) != 0
    assert len(settings) > 16

    settings = admin_user_client.list_setting(all=False)
    names = {x.name for x in settings}

    assert len(settings) != 0
    assert len(settings) < 16
    assert 'rancher.compose.linux.url' in names


def test_settings_list_install_uuid(admin_user_client):
    id = 'install.uuid'
    s = admin_user_client.by_id_setting(id)
    assert s is not None
    assert s.value is not None
