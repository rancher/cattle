import random
import dstack
import pytest
import time


SIM_EXTERNAL_POOL = "simexternalpool"

@pytest.fixture(scope="module")
def client():
    return dstack.from_env("DSAPI")


@pytest.fixture(scope="module")
def admin_client():
    return dstack.from_env("DSAPI")

@pytest.fixture(scope="module")
def sim_external_pool(admin_client):
    sim_pools = admin_client.list_storagePool(uuid=SIM_EXTERNAL_POOL)
    pool = None
    if len(sim_pools) == 0:
        pool = admin_client.create_storagePool(uuid=SIM_EXTERNAL_POOL, kind="sim", external=True)
    else:
        pool = sim_pools[0]

    pool = wait_success(admin_client, pool)
    if pool.state == "inactive":
        pool.activate()
        pool = wait_success(admin_client, pool)

    assert pool.state == "active"
    assert pool.kind == "sim"
    assert pool.external

    return pool


def random_num():
    return random.randint(0, 1000000);


def wait_success(client, obj):
    obj = wait_transitioning(client, obj)
    assert obj.transitioning == "no"
    return obj


def wait_transitioning(client, obj):
    obj = client.reload(obj)
    while obj.transitioning == "yes":
        time.sleep(.5)
        obj = client.reload(obj)

    return obj


