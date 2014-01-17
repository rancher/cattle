import random
import dstack
import pytest
import time


NOT_NONE = object()


def _admin_client():
    return dstack.from_env("DSAPI", access_key="admin", secrect_key="adminpass")


def _client_for_user(name, accounts):
    return dstack.from_env("DSAPI", access_key=accounts[name][0], secret_key=accounts[name][1])


@pytest.fixture(scope="module")
def accounts():
    result = {}
    admin_client = _admin_client()
    for user_name in ["admin", "agent", "user", "agentRegister", "test", "readAdmin"]:
        password = user_name + "pass"
        account = create_type_by_uuid(admin_client, "account", user_name,
                                      kind=user_name,
                                      name=user_name)

        active_cred = None
        for cred in account.credentials():
            if cred.publicValue == user_name and cred.secretValue == password:
                active_cred = cred
                break

        if active_cred is None:
            active_cred = admin_client.create_credential({
                "accountId": account.id,
                "kind": "apiKey",
                "publicValue": user_name,
                "secretValue": password
            })

        active_cred = wait_success(admin_client, active_cred)
        if active_cred.state != "active":
            wait_success(admin_client, active_cred.activate())

        result[user_name] = [user_name, password]

    #for setting in admin_client.list_setting():
    #    if setting.name == "api.security.enabled" and setting.activeValue != "true":
    #        admin_client.update(setting, {
    #            "value": "true"
    #        })

    return result


@pytest.fixture(scope="module")
def client(accounts):
    return _client_for_user("test", accounts)


@pytest.fixture(scope="module")
def admin_client(accounts):
    return _client_for_user("admin", accounts)


@pytest.fixture(scope="module")
def sim_host(admin_client, sim_agent):
    return create_type_by_uuid(admin_client, "host", "simhost1", kind="sim", agentId=sim_agent.id)


@pytest.fixture(scope="module")
def sim_external_pool(admin_client):
    return create_type_by_uuid(admin_client, "storagePool", "simexternalpool", kind="sim", external=True)


@pytest.fixture(scope="module")
def sim_pool(admin_client, sim_host, sim_agent):
    pool = create_type_by_uuid(admin_client, "storagePool", "simpool1", kind="sim", agentId=sim_agent.id)
    assert not pool.external

    create_type_by_uuid(admin_client, "storagePoolHostMap", "simpool1-simhost",
                        storagePoolId=pool.id, hostId=sim_host.id)

    return pool


@pytest.fixture(scope="module")
def sim_agent(admin_client):
    return create_type_by_uuid(admin_client, "agent", "simagent1", kind="sim", uri="sim://")


@pytest.fixture(scope="module")
def sim_context(sim_host, sim_pool, sim_external_pool, sim_agent):
    return {
        "host": sim_host,
        "pool": sim_pool,
        "external_pool": sim_external_pool,
        "agent": sim_agent
    }


def create_type_by_uuid(admin_client, type, uuid, activate=True, **kw):
    opts = dict(kw)
    opts["uuid"] = uuid

    objs = admin_client.list(type, uuid=uuid)
    obj = None
    if len(objs) == 0:
        obj = admin_client.create(type, **opts)
    else:
        obj = objs[0]

    obj = wait_success(admin_client, obj)
    if activate and obj.state == "inactive":
        obj.activate()
        obj = wait_success(admin_client, obj)

    for k, v in opts.items():
        assert getattr(obj, k) == v

    return obj


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


def assert_fields(obj, fields):
    assert obj is not None
    for k, v in fields.items():
        assert k in obj
        if v is None:
            assert obj[k] is None
        elif v is NOT_NONE:
            assert obj[k] is not None
        else:
            assert obj[k] == v
