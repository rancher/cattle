from common_fixtures import *  # NOQA


def test_create_ssh_key_default(admin_client):
    key = admin_client.create_ssh_key()
    assert key.state == 'registering'

    key = admin_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue.startswith('ssh-rsa ')
    assert key.publicValue.endswith('cattle@cattle')
    assert key.secretValue.startswith('-----BEGIN RSA PRIVATE KEY-----')


def test_create_ssh_key_with_value(admin_client):
    key = admin_client.create_ssh_key(publicValue='ssh-rsa')
    assert key.state == 'registering'

    key = admin_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue == 'ssh-rsa'
    assert key.secretValue is None


def test_create_container(admin_client, sim_context):
    key = create_and_activate(admin_client, 'sshKey')
    c = create_sim_container(admin_client, sim_context, credentialIds=[key.id])

    maps = c.credentialInstanceMaps()
    assert len(maps) == 1

    map = maps[0]

    assert map.state == 'active'
    assert map.credentialId == key.id
