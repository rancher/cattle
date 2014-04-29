from common_fixtures import *  # NOQA


def test_network_service_provider_create(admin_client):
    network = create_and_activate(admin_client, 'network')

    assert_required_fields(admin_client.create_network_service_provider,
                           networkId=network.id)

    create_and_activate(admin_client, 'networkServiceProvider',
                        networkId=network.id)
