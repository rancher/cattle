import dstack
import pytest
import time


@pytest.fixture(scope="module")
def client():
    return dstack.from_env("DSAPI")


@pytest.fixture(scope="module")
def admin_client():
    return dstack.from_env("DSAPI")


def wait_success(client, obj):
    obj = wait_transitioning(client, obj)
    assert obj.transitioning == "no"
    return obj


def wait_transitioning(client, obj):
    while obj.transitioning == "yes":
        time.sleep(.5)
        obj = client.reload(obj)

    return obj


