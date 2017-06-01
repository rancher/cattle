from common_fixtures import *  # NOQA
from test_docker import docker_client  # NOQA


@pytest.fixture(scope='module')
def service_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='service').user_client


def test_host_template_create(client, service_client):
    secret_value = {
        'bye': 2,
        'fooConfig': {
            'key1': 'value1'
        }
    }
    mds = client.create_host_template(name='foo-ht-test0',
                                      driver='foo',
                                      publicValues={
                                          'fooConfig': {
                                              'hi': 1,
                                          },
                                      },
                                      secretValues=secret_value)
    try:
        assert mds.secretValues == secret_value
        assert mds.publicValues == {'fooConfig': {'hi': 1}}

        mds = client.reload(mds)
        assert 'secretValues' not in mds.links
        assert mds.secretValues == {
            'bye': None,
            'fooConfig': {
                'key1': None,
            }
        }

        mds = client.update(mds, name='foobar')
        assert mds.name == 'foobar'

        mds_service = service_client.reload(mds)
        assert 'secretValues' in mds_service.links
        ret = mds_service.secretValues_link()
        assert ret == secret_value

    finally:
        mds = client.wait_success(client.delete(mds))
        assert mds.removed is not None
