from common_fixtures import *  # NOQA
import re


@pytest.mark.parametrize('kind', ['user', 'admin'])
def test_account_create(kind, admin_client, random_str):
    account = admin_client.create_account(kind=kind,
                                          name=random_str)

    assert account.state == "registering"
    assert account.transitioning == "yes"

    account = wait_success(admin_client, account)

    assert account.transitioning == "no"
    assert account.state == "active"

    count = len(admin_client.list_account(name=random_str))
    assert count == 1

    creds = account.credentials()

    assert len(creds) == 2
    creds = filter(lambda x: x.kind == 'apiKey', creds)

    assert len(creds) == 1
    assert creds[0].state == "active"
    assert creds[0].kind == "apiKey"
    assert re.match("[A-Z]*", creds[0].publicValue)
    assert len(creds[0].publicValue) == 20
    assert re.match("[a-zA-Z0-9]*", creds[0].secretValue)
    assert len(creds[0].secretValue) == 40


def test_account_external(admin_client):
    account = admin_client.create_account(externalId='extid',
                                          externalIdType='extType')
    account = admin_client.wait_success(account)

    assert account.state == 'active'
    assert account.externalId == 'extid'
    assert account.externalIdType == 'extType'


def test_account_no_key(super_admin_client):
    account = super_admin_client.create_account(kind='admin')
    account = super_admin_client.wait_success(account)
    creds = account.credentials()

    assert len(creds) >= 2

    account = super_admin_client.create_account(kind='unknown')
    account = super_admin_client.wait_success(account)
    creds = account.credentials()

    assert len(creds) == 0
