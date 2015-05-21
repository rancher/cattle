from common_fixtures import *  # NOQA


TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') == 'false'",
                               reason='DOCKER_TEST is not set')


def _create_registry(client):
    registry = client.create_registry(serverAddress='quay.io',
                                      name='Quay')
    registry = client.wait_success(registry)
    assert registry.serverAddress == 'quay.io'
    assert registry.name == 'Quay'

    return registry


def _create_registry_credential(client):
    registry = _create_registry(client)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        email='test@rancher.com',
        publicValue='rancher',
        secretValue='rancher')
    reg_cred = client.wait_success(reg_cred)
    assert reg_cred is not None
    assert reg_cred.email == 'test@rancher.com'
    assert reg_cred.kind == 'registryCredential'
    assert reg_cred.registryId == registry.id
    assert reg_cred.publicValue == 'rancher'
    assert 'secretValue' not in reg_cred

    return reg_cred


@if_docker
def _test_create_container_with_registry_credential(client, docker_context):
    reg_cred = _create_registry_credential(client)
    uuid = TEST_IMAGE_UUID
    container = client.create_container(name='test',
                                        imageUuid=uuid,
                                        startOnCreate=False,
                                        registryCredentialId=reg_cred.id)
    assert container is not None
    assert container.registryCredentialId == reg_cred.id
    assert container.startOnCreate is False
    assert container.imageUuid == uuid


@if_docker
def _test_create_container_with_real_registry_credential(client,
                                                         docker_context):
    reg_cred = _create_registry_credential(client)
    uuid = 'docker:registry.rancher.io/rancher/loop'
    container = client.create_container(name='test',
                                        imageUuid=uuid,
                                        registryCredentialId=reg_cred.id)
    assert container is not None
    assert container.registryCredentialId == reg_cred.id
    assert container.imageUuid == uuid

    container = client.wait_success(container)

    assert container.state == 'running'


def _crud_registry(client):
    registry = _create_registry(client)
    registry = client.wait_success(registry)
    assert registry.state == 'active'
    registry = client.wait_success(registry.deactivate())
    assert registry.state == 'inactive'
    registry = client.delete(registry)
    registry = client.wait_success(registry)
    assert registry.state == 'removed'
    registry = client.wait_success(registry.purge())
    assert registry.state == 'purged'


def _crud_registry_credential(client):
    registry_credential = _create_registry_credential(client)
    registry_credential = client.wait_success(registry_credential)
    assert registry_credential.state == 'active'
    registry_credential = client.wait_success(registry_credential.deactivate())
    assert registry_credential.state == 'inactive'
    registry_credential = client.wait_success(
        client.update(registry_credential, {
            'publicValue': 'test',
            'secretValue': 'rancher45',
            'email': 'engineering@rancher.com',
        }))
    assert registry_credential.publicValue == 'test'
    assert registry_credential.email == 'engineering@rancher.com'
    registry_credential = client.delete(registry_credential)
    registry_credential = client.wait_success(registry_credential)
    assert registry_credential.state == 'removed'
    registry_credential = client.wait_success(registry_credential.purge())
    assert registry_credential.state == 'purged'
    pass


def test_crud_registry(client):
    _crud_registry(client)


def test_crud_registry_credential(client):
    _crud_registry_credential(client)
