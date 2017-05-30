from common_fixtures import *  # NOQA
from gdapi import ApiError

TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') == 'false'",
                               reason='DOCKER_TEST is not set')


def _create_registry(client):
    server = 'server{0}.io'.format(random_num())
    registry = client.create_registry(serverAddress=server,
                                      name='Server')
    registry = client.wait_success(registry)
    assert registry.serverAddress == server
    assert registry.name == 'Server'

    return registry


def _create_registry_and_credential(client):
    registry = _create_registry(client)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        publicValue='rancher',
        secretValue='rancher')
    reg_cred = client.wait_success(reg_cred)
    assert reg_cred is not None
    assert reg_cred.kind == 'registryCredential'
    assert reg_cred.registryId == registry.id
    assert reg_cred.publicValue == 'rancher'
    assert reg_cred.secretValue is None

    return reg_cred, registry


@if_docker
def test_create_container_with_registry_credential(client, context):
    reg_cred, registry = _create_registry_and_credential(client)
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
    reg_cred, registry = _create_registry_and_credential(client)
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


def _crud_registry_credential(client):
    registry_credential, registry = _create_registry_and_credential(client)
    registry_credential = client.wait_success(registry_credential)
    assert registry_credential.state == 'active'
    registry_credential = client.wait_success(registry_credential.deactivate())
    assert registry_credential.state == 'inactive'
    registry_credential = client.wait_success(
        client.update(registry_credential, {
            'publicValue': 'test',
            'secretValue': 'rancher45',
        }))
    assert registry_credential.publicValue == 'test'
    registry_credential = client.delete(registry_credential)
    registry_credential = client.wait_success(registry_credential)
    assert registry_credential.state == 'removed'


def test_crud_registry(client):
    _crud_registry(client)


def test_crud_registry_credential(client):
    _crud_registry_credential(client)


def test_deleting_registry_deletes_credentials(client):
    reg_cred, registry = _create_registry_and_credential(client)
    registry = client.wait_success(registry.deactivate())
    registry = client.delete(registry)
    registry = client.wait_success(registry)
    assert registry.state == 'removed'

    def is_state():
        cred = client.reload(reg_cred)
        if (cred.state == 'removed'):
            return cred
        print cred.state
        return False

    reg_cred = wait_for(is_state)
    assert reg_cred.state == 'removed'


def test_container_image_and_registry_credential(client,
                                                 super_client):

    server = 'server{0}.io'.format(random_num())

    registry = client.create_registry(serverAddress=server,
                                      name=random_str())
    registry = client.wait_success(registry)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        publicValue='rancher',
        secretValue='rancher')
    registry_credential = client.wait_success(reg_cred)
    name = server + '/rancher/authorized:latest'
    image_uuid = 'docker:' + name
    container = client.create_container(imageUuid=image_uuid,
                                        name="test" + random_str(),
                                        startOnCreate=False)
    container = super_client.wait_success(container)
    assert container.registryCredentialId == registry_credential.id
    image = container.image()
    assert image.name == name
    assert image.registryCredentialId == registry_credential.id


def test_duplicate_server_addresses(client):
    server = 'server{0}.io'.format(random_num())
    registry = client.create_registry(serverAddress=server,
                                      name=random_str())
    client.wait_success(registry)
    with pytest.raises(ApiError) as e:
        client.create_registry(serverAddress=server, name=random_str())
    assert e.value.error.status == 400
    assert e.value.error.code == 'ServerAddressUsed'


def test_create_same_registry_different_projects(admin_user_client):
    server = 'server{0}.io'.format(random_num())
    context_1 = create_context(admin_user_client, create_project=True)
    context_2 = create_context(admin_user_client, create_project=True)

    context_1.client.wait_success(context_1.client.create_registry(
        serverAddress=server, name=random_str()))
    context_2.client.wait_success(context_2.client.create_registry(
        serverAddress=server, name=random_str()))


def test_registry_credentials(client, super_client, admin_user_client):
    registry = _create_registry(client)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        publicValue='rancher',
        secretValue='rancher')

    reg_cred = client.wait_success(reg_cred)
    assert reg_cred is not None
    assert reg_cred.secretValue is None
    projectadmin_client = create_context(admin_user_client,
                                         create_project=False,
                                         add_host=False,
                                         kind='projectadmin').user_client

    registry = _create_registry(projectadmin_client)
    reg_cred = projectadmin_client.create_registry_credential(
        registryId=registry.id,
        publicValue='rancher',
        secretValue='rancher')
    reg_cred = projectadmin_client.wait_success(reg_cred)
    assert reg_cred is not None
    assert reg_cred.secretValue is not None

    creds = client.list_registryCredential(publicValue=reg_cred.publicValue,
                                           _role='projectadmin')

    assert len(creds) >= 1
    assert creds[0].secretValue is None

    # only super admin can pass the role
    creds = super_client.list_registryCredential(
        publicValue=reg_cred.publicValue, _role='projectadmin')
    assert len(creds) >= 1
    assert creds[0].secretValue is not None

    # validate that you can't pass other roles than projectadmin
    creds = client.list_registryCredential(publicValue=reg_cred.publicValue,
                                           _role='admin')
    assert len(creds) >= 1
    assert creds[0].secretValue is None
