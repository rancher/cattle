import cattle
import os
import pytest
import random
import time
from datetime import datetime, timedelta


NOT_NONE = object()
DEFAULT_TIMEOUT = 45
DEFAULT_AGENT_URI = 'ssh://root@localhost:22'
DEFAULT_AGENT_UUID = 'test-agent'


def _admin_client():
    return cattle.from_env(access_key='admin',
                           secrect_key='adminpass')


def _client_for_user(name, accounts):
    return cattle.from_env(access_key=accounts[name][0],
                           secret_key=accounts[name][1])


@pytest.fixture(scope='module')
def accounts():
    result = {}
    admin_client = _admin_client()
    for user_name in ['admin', 'agent', 'user', 'agentRegister', 'test',
                      'readAdmin']:
        password = user_name + 'pass'
        account = create_type_by_uuid(admin_client, 'account', user_name,
                                      kind=user_name,
                                      name=user_name)

        active_cred = None
        for cred in account.credentials():
            if cred.publicValue == user_name and cred.secretValue == password:
                active_cred = cred
                break

        if active_cred is None:
            active_cred = admin_client.create_credential({
                'accountId': account.id,
                'kind': 'apiKey',
                'publicValue': user_name,
                'secretValue': password
            })

        active_cred = wait_success(admin_client, active_cred)
        if active_cred.state != 'active':
            wait_success(admin_client, active_cred.activate())

        result[user_name] = [user_name, password, account]

    return result


@pytest.fixture(scope='module')
def admin_account(accounts):
    return accounts['admin'][2]


@pytest.fixture(scope='module')
def client(accounts):
    return _client_for_user('user', accounts)


@pytest.fixture(scope='module')
def admin_client(accounts):
    return _client_for_user('admin', accounts)


@pytest.fixture(scope='module')
def sim_context(admin_client):
    context = kind_context(admin_client, 'sim', external_pool=True,
                           uri='sim://', uuid='simagent1')
    context['imageUuid'] = 'sim:{}'.format(random_num())

    return context


@pytest.fixture(scope='module')
def sim_context2(admin_client):
    context = kind_context(admin_client, 'sim', external_pool=True,
                           uri='sim://2', uuid='simagent2')
    context['imageUuid'] = 'sim:{}'.format(random_num())

    return context


@pytest.fixture(scope='module')
def sim_context3(admin_client):
    context = kind_context(admin_client, 'sim', external_pool=True,
                           uri='sim://3', uuid='simagent3')
    context['imageUuid'] = 'sim:{}'.format(random_num())

    return context


def activate_resource(admin_client, obj):
    if obj.state == 'inactive':
        obj = wait_success(admin_client, obj.activate())

    return obj


def find_by_uuid(admin_client, type, uuid, activate=True, **kw):
    objs = admin_client.list(type, uuid=uuid)
    assert len(objs) == 1

    obj = wait_success(admin_client, objs[0])

    if activate:
        return activate_resource(admin_client, obj)

    return obj


def create_type_by_uuid(admin_client, type, uuid, activate=True, validate=True,
                        **kw):
    opts = dict(kw)
    opts['uuid'] = uuid

    objs = admin_client.list(type, uuid=uuid)
    obj = None
    if len(objs) == 0:
        obj = admin_client.create(type, **opts)
    else:
        obj = objs[0]

    obj = wait_success(admin_client, obj)
    if activate and obj.state == 'inactive':
        obj.activate()
        obj = wait_success(admin_client, obj)

    if validate:
        for k, v in opts.items():
            assert getattr(obj, k) == v

    return obj


def random_num():
    return random.randint(0, 1000000)


def random_str():
    return 'random-{0}'.format(random_num())


def wait_all_success(client, objs, timeout=DEFAULT_TIMEOUT):
    ret = []
    for obj in objs:
        obj = wait_success(client, obj, timeout)
        ret.append(obj)

    return ret


def wait_success(client, obj, timeout=DEFAULT_TIMEOUT):
    return client.wait_success(obj, timeout=timeout)


def wait_transitioning(client, obj, timeout=DEFAULT_TIMEOUT):
    return client.wait_transitioning(obj, timeout=timeout)


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


def assert_removed_fields(obj):
    assert obj.removed is not None
    assert obj.removeTime is not None

    assert obj.removeTimeTS > obj.removedTS


def assert_restored_fields(obj):
    assert obj.removed is None
    assert obj.removeTime is None


def now():
    return datetime.utcnow()


def format_time(time):
    return (time - timedelta(microseconds=time.microsecond)).isoformat() + 'Z'


def get_agent(admin_client, name, default_uri=DEFAULT_AGENT_URI,
              default_agent_uuid=DEFAULT_AGENT_UUID):
    name = name.upper()
    uri_name = '{0}_URI'.format(name.upper())
    uuid_name = '{0}_AGENT_UUID'.format(name.upper())

    uri = os.getenv(uri_name, default_uri)
    uuid = os.getenv(uuid_name, default_agent_uuid)

    agent = create_type_by_uuid(admin_client, 'agent', uuid, validate=False,
                                uri=uri)

    while len(agent.hosts()) == 0:
        time.sleep(0.5)

    return agent


def kind_context(admin_client, kind, external_pool=False,
                 uri=DEFAULT_AGENT_URI,
                 uuid=DEFAULT_AGENT_UUID):
    kind_agent = get_agent(admin_client, kind, default_agent_uuid=uuid,
                           default_uri=uri)

    hosts = filter(lambda x: x.kind == kind, kind_agent.hosts())
    assert len(hosts) == 1
    kind_host = activate_resource(admin_client, hosts[0])

    pools = kind_host.storagePools()
    assert len(pools) == 1
    kind_pool = activate_resource(admin_client, pools[0])

    context = {
        'host': kind_host,
        'pool': kind_pool,
        'agent': kind_agent
    }

    if external_pool:
        pools = admin_client.list_storagePool(kind=kind, external=True)
        assert len(pools) == 1
        context['external_pool'] = activate_resource(admin_client, pools[0])

    return context


def assert_required_fields(method, **kw):
    method(**kw)

    for k in kw.keys():
        args = dict(kw)
        del args[k]

        try:
            method(**args)
            # This is supposed to fail
            assert k == ''
        except cattle.ApiError as e:
            assert e.error.code == 'MissingRequired'
            assert e.error.fieldName == k


def get_plain_id(admin_client, obj):
    ret = admin_client.list(obj.type, uuid=obj.uuid, _plainId='true')
    assert len(ret) == 1
    return ret[0].id
