from common_fixtures import *
import time
import random
import re


def test_account_create(client):
    cred_count = len(client.list_credential())
    count = len(client.list_account())
    account = client.create_account()

    assert account.state == "registering"
    assert account.transitioning == "yes"

    account = wait_success(client, account)

    assert account.transitioning == "no"
    assert account.state == "active"

    new_count = len(client.list_account())
    assert (count+1) == new_count

    new_count = len(client.list_credential())
    assert (cred_count+1) == new_count

    creds = account.credentials()

    assert len(creds) == 1
    assert creds[0].state == "active"
    assert creds[0].kind == "apiKey"
    assert re.match("[A-Z]*", creds[0].publicValue)
    assert len(creds[0].publicValue) == 20
    assert re.match("[a-zA-Z0-9]*", creds[0].secretValue)
    assert len(creds[0].secretValue) == 40

