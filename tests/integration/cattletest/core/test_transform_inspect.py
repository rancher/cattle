from common_fixtures import *  # NOQA
import requests
from requests.auth import HTTPBasicAuth
import urlparse
import json

RESOURCE_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            'resources')


@pytest.fixture(scope='module')
def transform_url(client):
    return urlparse.urljoin(client.schema.types['schema'].links['collection'],
                            'scripts/transform')


def test_transform_inspect_minimal(transform_url, client):
    response = requests.post(transform_url,
                             '{"Id": "an-id", '
                             '"Config": {"Image": "image" }, '
                             '"HostConfig": {}}',
                             auth=HTTPBasicAuth(client._access_key,
                                                client._secret_key))
    assert response.status_code == 200
    container = response.json()
    assert container['name'] is None
    assert container['externalId'] == 'an-id'
    assert container['imageUuid'] == 'docker:image'
    assert container['kind'] == 'container'
    assert container['hostname'] is None
    assert container['user'] is None
    assert container['memory'] is None
    assert container['memorySwap'] is None
    assert container['cpuShares'] is None
    assert container['cpuSet'] is None
    assert container['tty'] is False
    assert container['stdinOpen'] is False
    assert container['directory'] is None
    assert len(container['environment']) == 0
    assert len(container['command']) == 0
    assert len(container['dataVolumes']) == 0
    assert container['privileged'] is False
    assert len(container['dns']) == 0
    assert len(container['dnsSearch']) == 0
    assert len(container['capAdd']) == 0
    assert len(container['capDrop']) == 0
    assert container['restartPolicy'] is None
    assert len(container['devices']) == 0


def test_transform_inspect_full(transform_url, client, super_client):
    inspect = inspect_payload('inspect_full.json')
    response = requests.post(transform_url, inspect,
                             auth=HTTPBasicAuth(client._access_key,
                                                client._secret_key))
    assert response.status_code == 200
    container = response.json()
    assert container is not None
    assert container['name'] == 'boring_euclid'
    assert container['hostname'] == 'ahostname'
    assert container['externalId'] == 'f13e0c667453e85c0409022127f750abe4ea4' \
                                      'e517a970b22cff523def62acc28'
    assert container['kind'] == 'container'
    assert container['domainName'] == 'domain.com'
    assert container['user'] is None
    assert container['memory'] == 4194304
    assert container['memorySwap'] is None
    assert container['cpuShares'] == 1
    assert container['cpuSet'] is None
    assert container['tty'] is True
    assert container['stdinOpen'] is True
    assert container['imageUuid'] == 'docker:busybox'
    assert container['directory'] == '/a_workdir'
    assert len(container['environment']['ENV1']) == 3
    assert container['environment']['ENV1'] == 'FOO'
    assert container['environment']['NOVALUE'] == ''
    assert 'PATH' in container['environment']
    assert container['entryPoint'] == ['/bin/bash']
    assert container['command'] == ['sleep', '1', '2', '3']
    assert len(container['dataVolumes']) == 3
    assert '/tmpcontainer2' in container['dataVolumes']
    assert '/tmp:/tmpcontainer' in container['dataVolumes']
    assert '/tmp3:/tmpcontainer3:ro' in container['dataVolumes']
    assert container['privileged'] is True
    assert container['publishAllPorts'] is False
    assert container['dns'] == ['10.42.1.2']
    assert container['dnsSearch'] == ['search.dns.com']
    assert len(container['capAdd']) == 1
    assert 'NET_ADMIN' in container['capAdd']
    assert len(container['capDrop']) == 1
    assert 'MKNOD' in container['capDrop']
    restart_pol = container['restartPolicy']
    assert restart_pol['name'] == 'on-failure'
    assert restart_pol['maximumRetryCount'] == 10
    assert len(container['devices']) == 1
    assert '/dev/null:/dev/xnull:rwm' in container['devices']

    # Load with admin so that we can see the data field
    response = requests.post(transform_url, inspect,
                             auth=HTTPBasicAuth(super_client._access_key,
                                                super_client._secret_key))
    assert response.status_code == 200
    container = response.json()
    ports = container['data']['fields']['ports']
    assert len(ports) == 2
    assert '2222:1111/tcp' in ports
    assert '3333/udp' in ports


def test_transform_inspect_rountrip(transform_url, client):
    inspect = inspect_payload('roundtrip_inspect.json')
    response = requests.post(transform_url, inspect,
                             auth=HTTPBasicAuth(client._access_key,
                                                client._secret_key))
    assert response.status_code == 200
    container = response.json()

    # orig_container = None
    with open(os.path.join(RESOURCE_DIR,
                           'roundtrip_container.json')) as data_file:
        orig_container = json.load(data_file)

    assert orig_container is not None

    # In this roundtrip, names won't match because the docker container was
    # given the uuid as the name
    assert container['name'] == orig_container['uuid']
    assert container['hostname'] == orig_container['hostname']
    assert container['externalId'] == orig_container['externalId']
    assert container['kind'] == orig_container['kind']
    assert container['domainName'] == orig_container['domainName']
    assert container['user'] == orig_container['user']
    assert container['memory'] == orig_container['memory']
    assert container['memorySwap'] == orig_container['memorySwap']
    assert container['cpuShares'] == orig_container['cpuShares']
    assert container['cpuSet'] == orig_container['cpuSet']
    assert container['tty'] == orig_container['tty']
    assert container['stdinOpen'] == orig_container['stdinOpen']
    assert container['imageUuid'] == orig_container['imageUuid']
    assert container['directory'] == orig_container['directory']
    assert container['environment'] == orig_container['environment']
    assert container['command'] == orig_container['command']
    orig_vols = {}
    for vol in orig_container['dataVolumes']:
        orig_vols[vol] = True
    new_vols = {}
    for vol in container['dataVolumes']:
        new_vols[vol.replace(':rw', '')] = True
    assert orig_vols == new_vols

    assert container['privileged'] == orig_container['privileged']
    assert container['publishAllPorts'] == orig_container['publishAllPorts']
    assert container['dns'] == orig_container['dns']
    assert container['dnsSearch'] == orig_container['dnsSearch']
    assert container['capAdd'] == orig_container['capAdd']
    assert container['capDrop'] == orig_container['capDrop']
    assert container['restartPolicy'] == orig_container['restartPolicy']
    assert container['devices'] == orig_container['devices']


def inspect_payload(name):
    with open(os.path.join(RESOURCE_DIR, name)) as f:
        return f.read()
