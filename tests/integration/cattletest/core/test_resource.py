from common_fixtures import *  # NOQA


#test that admin_client can list accounts (parent of project which has just been created)
def test_list_account(admin_client):
    admin_client.list_account()
