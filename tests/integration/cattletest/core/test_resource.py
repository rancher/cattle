from common_fixtures import *  # NOQA


#test that admin_client and client(user) can create a project and list it
def test_list_account(admin_client):
    admin_client.list_accounts()
    