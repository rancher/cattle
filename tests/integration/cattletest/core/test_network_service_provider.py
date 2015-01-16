from common_fixtures import *  # NOQA


def test_network_service_provider_create(internal_test_client):
    network = create_and_activate(internal_test_client, 'network')

    assert_required_fields(
        internal_test_client.create_network_service_provider,
        networkId=network.id)

    create_and_activate(internal_test_client, 'networkServiceProvider',
                        networkId=network.id)
