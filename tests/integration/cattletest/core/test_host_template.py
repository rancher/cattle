from common_fixtures import *  # NOQA
from test_docker import docker_client  # NOQA


@pytest.fixture(scope='module')
def service_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='service').user_client


def test_host_template_create(client, service_client):
    secret_value = {
        'bye': 2,
        'nested': {
            'key1': 'value1'
        }
    }
    mds = client.create_host_template(name='foo',
                                      publicValues={
                                          'hi': 1,
                                      },
                                      secretValues=secret_value)
    assert mds.secretValues == secret_value
    assert mds.publicValues == {'hi': 1}

    mds = client.reload(mds)
    assert 'secretValues' not in mds.links
    assert mds.secretValues == {
        'bye': None,
        'nested': {
            'key1': None,
        }
    }

    mds = client.update(mds, name='foobar')
    assert mds.name == 'foobar'

    mds_service = service_client.reload(mds)
    assert 'secretValues' in mds_service.links
    ret = mds_service.secretValues_link()
    assert ret == secret_value

    client.delete(mds)
    mds = client.wait_success(mds)
    assert mds.removed is not None


def _ignore_test_host_from_host_template(docker_client):  # NOQA
    for i in docker_client.list_host_template():
        docker_client.delete(i)

    ht = docker_client.create_host_template(
        publicValues={
            'digitaloceanConfig': {
                'region': 'sfo1',
                'size': '1gb',
            },
        },
        secretValues={
            'digitaloceanConfig': {
                'accessToken': 'XXXXX',
            },
        },
    )
    ht = docker_client.wait_success(ht)
    assert ht.state == 'active'

    host = docker_client.create_host(hostname='lala',
                                     digitaloceanConfig={},
                                     hostTemplateId=ht.id)
    host = docker_client.wait_success(host)
    assert host.state == 'active'
