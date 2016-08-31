from common_fixtures import *  # NOQA
import requests


def test_create_ssh_key_default(super_client):
    key = super_client.create_ssh_key()
    assert key.state == 'registering'

    key = super_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue.startswith('ssh-rsa ')
    assert key.publicValue.endswith('cattle@cattle')
    assert key.secretValue.startswith('-----BEGIN RSA PRIVATE KEY-----')
    assert 'pem' in key.links

    pem = requests.get(key.links['pem'], auth=('superadmin',
                                               'superadminpass')).text

    assert pem.startswith('-----BEGIN RSA PRIVATE KEY-----')


def test_create_ssh_key_with_value(super_client):
    key = super_client.create_ssh_key(publicValue='ssh-rsa')
    assert key.state == 'registering'

    key = super_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue == 'ssh-rsa'
    assert key.secretValue is None
    assert 'pem' not in key.links


def test_create_container(super_client, context):
    key = create_and_activate(super_client, 'sshKey')
    c = context.super_create_container(credentialIds=[key.id])
    c = super_client.reload(c)

    maps = c.credentialInstanceMaps()
    assert len(maps) == 1

    map = maps[0]

    assert map.state == 'active'
    assert map.credentialId == key.id
