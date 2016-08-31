from common_fixtures import *  # NOQA


# test that admin_client can list accounts
# (parent of project which has just been created)
def test_list_account(client):
    client.list_account()
