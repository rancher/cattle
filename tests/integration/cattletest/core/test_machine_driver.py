from common_fixtures import *  # NOQA


def test_machine_driver_name(admin_user_client):
    n = random_str().replace('-', 'F')
    url = 'http://foo/bar/docker-machine-driver-{}-v1.0-tar.gz'.format(n)
    md = admin_user_client.create_machine_driver(url=url)
    assert md.name == n
