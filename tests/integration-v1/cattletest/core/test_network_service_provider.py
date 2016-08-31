from common_fixtures import *  # NOQA


def test_network_service_provider_create(super_client):
    network = create_and_activate(super_client, 'network')

    assert_required_fields(super_client.create_network_service_provider,
                           networkId=network.id)

    create_and_activate(super_client, 'networkServiceProvider',
                        networkId=network.id)
