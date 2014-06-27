from common_fixtures import *  # NOQA


def test_account_default_fields(admin_client):
    schema = admin_client.schema.types['account']

    cred = schema.resourceFields.defaultCredentialIds
    network = schema.resourceFields.defaultNetworkIds

    assert schema is not None
    assert cred.type == 'array[reference[credential]]'
    assert cred.nullable
    assert cred.create
    assert cred.update

    assert network.type == 'array[reference[network]]'
    assert network.nullable
    assert network.create
    assert network.update
