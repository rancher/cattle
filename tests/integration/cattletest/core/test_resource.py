from common_fixtures import *  # NOQA


# test that admin_client can list accounts
# (parent of project which has just been created)
def test_list_account(client):
    client.list_account()


def test_schema_includeable_links(client):
    schema = client.schema.types['service']
    assert 'serviceLogs' in schema.includeableLinks
