from common_fixtures import *  # NOQA


def test_network_service_create(super_client):
    network = create_and_activate(super_client, 'network')

    assert_required_fields(super_client.create_network_service,
                           networkId=network.id)

    create_and_activate(super_client, 'networkService',
                        networkId=network.id)


def test_dns_service_create(super_client):
    network = create_and_activate(super_client, 'network')

    dns = create_and_activate(super_client, 'dnsService',
                              dns=['8.8.8.8', '8.8.4.4'],
                              networkId=network.id)

    assert dns.state == 'active'
    assert dns.dns == ['8.8.8.8', '8.8.4.4']


def test_metadata_service(super_client):
    network = super_client.create_network()
    meta_data = super_client.create_metadata_service(networkId=network.id)

    assert meta_data.configDrive is False

    meta_data = super_client.create_metadata_service(networkId=network.id,
                                                     configDrive=True)
    assert meta_data.configDrive
