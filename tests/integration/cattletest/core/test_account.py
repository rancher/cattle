from common_fixtures import *  # NOQA
import re
from cattle import from_env


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


def test_account_delete(super_client, random_str, admin_client):

    cred = create_user(admin_client,
                       random_str,
                       kind='user')

    test_user_client = from_env(url=cattle_url(),
                                cache=False,
                                access_key=cred[0],
                                secret_key=cred[1])

    user_account = cred[2]

    sim_context = create_sim_context(super_client,
                                     random_str,
                                     ip='192.168.11.6',
                                     account=user_account)

    host = super_client.wait_success(sim_context['host'])
    assert host.state == 'active'
    assert host.accountId == user_account.id
    account = test_user_client.wait_success(user_account.deactivate())
    account = test_user_client.delete(account)
    account = test_user_client.wait_success(account)
    account = test_user_client.wait_success(account.purge())
    host = admin_client.wait_success(sim_context["host"])
    assert host.state == 'removed'
