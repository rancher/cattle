import re
import time
from common_fixtures import *  # NOQA


TEST_IMAGE = 'ibuildthecloud/helloworld'
TEST_IMAGE_LATEST = TEST_IMAGE + ':latest'
TEST_IMAGE_UUID = 'docker:' + TEST_IMAGE

if_docker = pytest.mark.skipif("os.environ.get('DOCKER_TEST') == 'false'",
                               reason='DOCKER_TEST is not set')


@pytest.fixture(scope='session')
def docker_context(super_client):
    for host in super_client.list_host(state='active', remove_null=True,
                                       kind='docker'):
        return kind_context(super_client, 'docker', external_pool=True,
                            agent=host.agent())

    raise Exception('Failed to find docker host, please register one')


def _check_path(volume, should_exist, super_client):
    path = _path_to_volume(volume)
    c = super_client. \
        create_container(name="volume_check",
                         imageUuid="docker:cjellick/rancher-test-tools",
                         startOnCreate=False,
                         environment={'TEST_PATH': path},
                         command='/opt/tools/check_path_exists.sh',
                         dataVolumes=[
                             '/var/lib/docker:/host/var/lib/docker',
                             '/tmp:/host/tmp'])
    c.start()
    c = super_client.wait_success(c)
    c = _wait_until_stopped(c, super_client)

    code = c.data.dockerInspect.State.ExitCode
    if should_exist:
        # The exit code of the container should be a 10 if the path existed
        assert code == 10
    else:
        # And 11 if the path did not exist
        assert code == 11

    c.remove()


def _path_to_volume(volume):
    path = volume.uri.replace('file://', '')
    mounted_path = re.sub('^.*?/var/lib/docker', '/host/var/lib/docker',
                          path)
    if not mounted_path.startswith('/host/var/lib/docker'):
        mounted_path = re.sub('^.*?/tmp', '/host/tmp',
                              path)
    return mounted_path


def _wait_until_stopped(container, admin_client, timeout=45):
        start = time.time()
        container = admin_client.reload(container)
        while container.state != 'stopped':
            time.sleep(.5)
            container = admin_client.reload(container)
            if time.time() - start > timeout:
                raise Exception('Timeout waiting for container to stop.')

        return container


def _create_registry(client):
    registry = client.create_registry(serverAddress='quay.io',
                                      name='Quay')
    assert registry.serverAddress == 'quay.io'
    assert registry.name == 'Quay'

    return registry


def _create_registry_credential(client, super_client, docker_context,):
    registry = _create_registry(client)
    reg_cred = client.create_registry_credential(
        storagePoolId=registry.id,
        email='test@rancher.com',
        publicValue='wizardofmath+whisper',
        secretValue='W0IUYDBM2VORHM4DTTEHSMKLXGCG3KD3IT081QWWTZA11R9DZS2DDPP72'
                    '48NUTT6')
    assert reg_cred is not None
    assert reg_cred.email == 'test@rancher.com'
    assert reg_cred.kind == 'registryCredential'
    assert reg_cred.storagePoolId == registry.id
    assert reg_cred.publicValue == 'wizardofmath+whisper'
    assert 'secretValue' not in reg_cred

    return reg_cred


@if_docker
def test_create_container_with_registry_credential(client, super_client,
                                                   docker_context):
    reg_cred = _create_registry_credential(client, super_client,
                                           docker_context)
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
def test_create_container_with_real_registry_credential(client, super_client,
                                                        docker_context):
    reg_cred = _create_registry_credential(client, super_client,
                                           docker_context)
    uuid = 'docker:quay.io/wizardofmath/whisperdocker'
    container = client.create_container(name='test',
                                        imageUuid=uuid,
                                        registryCredentialId=reg_cred.id)
    assert container is not None
    assert container.registryCredentialId == reg_cred.id
    assert container.imageUuid == uuid

    container = client.wait_success(container)

    assert container.state == 'running'

