from common_fixtures import *  # NOQA
import requests


def test_create_ssh_key_default(internal_test_client):
    key = internal_test_client.create_ssh_key()
    assert key.state == 'registering'

    key = internal_test_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue.startswith('ssh-rsa ')
    assert key.publicValue.endswith('cattle@cattle')
    assert key.secretValue.startswith('-----BEGIN RSA PRIVATE KEY-----')
    assert 'pem' in key.links

    pem = requests.get(key.links['pem'], auth=('internalTest',
                                               'internalTestpass')).text

    assert pem.startswith('-----BEGIN RSA PRIVATE KEY-----')


def test_create_ssh_key_with_value(internal_test_client):
    key = internal_test_client.create_ssh_key(publicValue='ssh-rsa')
    assert key.state == 'registering'

    key = internal_test_client.wait_success(key)
    assert key.state == 'active'

    assert key.publicValue == 'ssh-rsa'
    assert key.secretValue is None
    assert 'pem' not in key.links


def test_create_container(internal_test_client, sim_context):
    key = create_and_activate(internal_test_client, 'sshKey')
    c = create_sim_container(internal_test_client, sim_context,
                             credentialIds=[key.id])

    maps = c.credentialInstanceMaps()
    assert len(maps) == 1

    map = maps[0]

    assert map.state == 'active'
    assert map.credentialId == key.id
