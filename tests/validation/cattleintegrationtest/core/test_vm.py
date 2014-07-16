from common_fixtures import *  # NOQA
import paramiko
import base64
from StringIO import StringIO


TEST_VM_IMAGE_UUID = 'cirros'


@pytest.fixture(scope='session')
def ssh_key(client):
    keys = client.list_ssh_key(uuid='defaultSshKey')
    assert len(keys) == 1
    assert keys[0].secretValue is not None

    return paramiko.RSAKey.from_private_key(StringIO(keys[0].secretValue))


def run_command(client, instance, ssh_key, user='cirros'):
    c = client.create_container(name=test_name() + '-ssh',
                                imageUuid=TEST_IMAGE_UUID,
                                ports=['2222:22'],
                                privileged=True,
                                instanceLinks={
                                    'ssh': instance.id
                                })
    c = client.wait_success(c)
    assert c.state == 'running'

    port = None
    for p in c.ports():
        if p.privatePort == 22:
            port = p
            break

    assert port is not None

    client = paramiko.SSHClient()
    client.connect(port.publicIpAddress().address,
                   username=user,
                   port=port.publicPort,
                   pkey=ssh_key)
    stdin, stdout, stderr = client.exec_command('ls')
    for line in stdout:
        print '... ' + line.strip('\n')
    client.close()


def test_vm(client, ssh_key, test_name):
    vm = client.create_virtual_machine(name=test_name,
                                       memoryMb=64,
                                       ports=['22'],
                                       imageUuid=TEST_VM_IMAGE_UUID)
    vm = client.create_virtual_machine(name=test_name,
                                       memoryMb=64,
                                       ports=['22'],
                                       imageUuid=TEST_VM_IMAGE_UUID)
    vm = client.wait_success(vm)

    assert vm.state == 'running'

    time.sleep(30)
    run_command(client, vm, ssh_key)

    #delete_all(client, [vm])
