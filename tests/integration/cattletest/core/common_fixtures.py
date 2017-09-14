import base64
import cattle
import os
import pytest
import random
import time
import inspect
from datetime import datetime, timedelta
import requests
import fcntl
import logging

NOT_NONE = object()
DEFAULT_TIMEOUT = 15
cattle.DEFAULT_TIMEOUT = 15
_SUPER_CLIENT = None


@pytest.fixture(scope='session', autouse=os.environ.get('DEBUG'))
def log():
    logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope='session')
def cattle_url(project_id=None):
    default_url = 'http://localhost:8080/v1/schemas'
    url = os.environ.get('CATTLE_URL', default_url).replace('/v1', '/v2')
    if project_id is None:
        return url
    if url.endswith('/schemas'):
        url = url[:len(url)-8]
    return '{}/projects/{}/schemas'.format(url, project_id)


@pytest.fixture(scope='function')
def new_context(admin_user_client, request):
    ctx = create_context(admin_user_client, create_project=True,
                         add_host=True)
    request.addfinalizer(lambda: cleanup_context(admin_user_client, ctx))
    return ctx


@pytest.fixture(scope='session')
def context(admin_user_client, request):
    return new_context(admin_user_client, request)


@pytest.fixture(scope='session')
def client(context):
    return context.client


@pytest.fixture(scope='session')
def system_account(super_client):
    return super_client.list_account(kind='system', uuid='system')[0]


@pytest.fixture(scope='session')
def super_account(super_client):
    return super_client.list_account(kind='superadmin', uuid='superadmin')[0]


@pytest.fixture(scope='session')
def admin_user_client(super_client):
    admin_account = super_client.list_account(kind='admin', uuid='admin')[0]
    key = super_client.create_api_key(accountId=admin_account.id)
    super_client.wait_success(key)

    client = api_client(key.publicValue, key.secretValue)
    init(client)
    return client


@pytest.fixture(scope='session')
def super_client(request):
    return _get_super_client(request)


def init(admin_user_client):
    kv = {
        'task.process.replay.schedule': '2',
        'task.config.item.migration.schedule': '5',
        'task.config.item.source.version.sync.schedule': '5',
        'allowed.user.agent.uri.prefix': 'sim://',
    }
    for k, v in kv.items():
        admin_user_client.create_setting(name=k, value=v)


@pytest.fixture
def random_str():
    return 'random-{0}-{1}'.format(random_num(), int(time.time()))


class Context(object):
    def __init__(self, account=None, project=None, user_client=None,
                 client=None, host=None, agent_client=None, agent=None,
                 owner_client=None):
        self.account = account
        self.project = project
        self.agent = agent
        self.user_client = user_client
        self.agent_client = agent_client
        self.client = client
        self.host = host
        self.image_uuid = '{}'.format(random_str())
        self.lb_v1_image_uuid = 'rancher/load-balancer-service'
        self.host_ip = None
        self.owner_client = owner_client
        if self.host is not None:
            self.host_ip = self.host.agentIpAddress

    def create_container(self, *args, **kw):
        c = self.create_container_no_success(*args, **kw)
        c = self.client.wait_success(c)
        try:
            if not kw['startOnCreate']:
                assert c.state == 'stopped'
                return c
        except KeyError:
            pass
        assert c.state == 'running'
        return c

    def create_container_no_success(self, *args, **kw):
        return self._create_container(self.client, *args, **kw)

    def _create_container(self, client, *args, **kw):
        if 'imageUuid' not in kw:
            kw['imageUuid'] = self.image_uuid
        c = client.create_container(*args, **kw)
        # Make sure it's waited for and reloaded w/ project client
        return self.client.wait_transitioning(c)

    def super_create_container(self, *args, **kw):
        c = self.super_create_container_no_success(*args, **kw)
        return self.client.wait_success(c)

    def super_create_container_no_success(self, *args, **kw):
        kw['accountId'] = self.project.id
        return self._create_container(super_client(None), *args, **kw)

    def delete(self, obj):
        if obj is None:
            return
        self.client.delete(obj)
        self.client.wait_success(obj)

    def wait_for_state(self, obj, state):
        obj = self.client.wait_success(obj)
        assert obj.state == state
        return obj


def create_context(admin_user_client, create_project=False, add_host=False,
                   kind=None, name=None):
    now = time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime())
    if name is None:
        name = 'Session {} Integration Test User {}' \
            .format(os.getpid(), now)
        project_name = 'Session {} Integration Test Project {}' \
            .format(os.getpid(), now)
    else:
        project_name = name + '\'s Project {}'.format(now)

    if kind is None:
        kind = 'user'

    account = admin_user_client.create_account(name=name, kind=kind)
    account = admin_user_client.wait_success(account)
    key = admin_user_client.create_api_key(accountId=account.id)
    admin_user_client.wait_success(key)
    user_client = api_client(key.publicValue, key.secretValue)

    try:
        account = user_client.reload(account)
    except KeyError:
        # The account type can't see the account obj
        pass

    project = None
    project_client = None
    agent_client = None
    agent = None
    owner_client = None

    if create_project:
        project = user_client.create_project(name=project_name, members=[{
            'role': 'owner',
            'externalId': acc_id(user_client),
            'externalIdType': 'rancher_id'
        }])
        project = user_client.wait_success(project)
        # This is not proper yet because basic auth can't be used w/ Projects
        project_key = admin_user_client.create_api_key(accountId=project.id)
        admin_user_client.wait_success(project_key)
        project_client = api_client(project_key.publicValue,
                                    project_key.secretValue)
        project = project_client.reload(project)
        owner_client = api_client(key.publicValue, key.secretValue,
                                  project_id=project.id)
        _create_network_driver(owner_client)

    if create_project and add_host:
        host, agent, agent_client = \
            register_simulated_host(project_client, return_agent=True)
    else:
        host = None

    return Context(account=account, project=project, user_client=user_client,
                   client=project_client, host=host,
                   agent_client=agent_client, agent=agent,
                   owner_client=owner_client)


def _create_network_driver(client):
    driver_name = 'default-test-driver'
    stack = client.create_stack(name=driver_name, system=True)
    s = client.create_network_driver_service(
        name=driver_name,
        startOnCreate=True,
        stackId=stack.id,
        selectorContainer='none',
        networkDriver={
            'name': driver_name,
            'defaultNetwork': {
                'subnets': [
                    {
                        'networkAddress': '10.42.0.0/16'
                    }
                ],
                'dns': ['169.254.169.250'],
                'dnsSearch': ['rancher.internal'],
            },
            'cniConf': {},
        })

    s = client.wait_success(s)
    assert s.state == 'active'

    nd = find_one(client.list_network_driver, serviceId=s.id, name=driver_name)
    nd = client.wait_success(nd)
    assert nd.state == 'active'

    network = find_one(nd.networks)
    network = client.wait_success(network)
    assert network.state == 'active'


def cleanup_context(admin_user_client, context):
    purge_account(admin_user_client, context.project)
    purge_account(admin_user_client, context.account)


def purge_account(admin_user_client, account):
    account = admin_user_client.reload(account)
    for action in ['deactivate', 'remove', 'purge']:
        if action not in account:
            continue

        try:
            account = account[action]()
            if action != 'purge':
                admin_user_client.wait_success(account)
        except:
            pass


def register_simulated_host(client_or_context, return_agent=False):
    client = client_or_context
    if isinstance(client_or_context, Context):
        client = client_or_context.client

    def check():
        hosts = super_client(None).list_host(agentId=agents[0].id)
        if len(hosts) > 0:
            assert len(hosts) == 1
            return hosts[0]

    token = client.wait_success(client.create_registration_token())
    c = api_client('registrationToken', token.token)
    key = random_str()

    # Now this where we hack things up to make it a simulator
    s = super_client(None)
    register = s.create_register(key=key,
                                 accountId=token.accountId,
                                 agentUriFormat='sim://%s')
    # End hacking...

    try:
        register = c.wait_success(register)
    except:
        print register
        print process_instances(s, register)
        raise
    register = c.list_register(key=key)[0]

    c = api_client(register.accessKey, register.secretKey)
    agents = c.list_agent()

    keys = s.list_credential(publicValue=register.accessKey)
    assert len(keys) == 1

    assert len(agents) == 1

    s.update(agents[0], uri='sim://{}'.format(key))

    host = wait_for(check)
    host = client.wait_success(host)
    s.wait_success(agents[0])

    try:
        host = wait_state(client, host, 'active')
    except:
        print client.list_host()
        print process_instances(s, host)
        raise
    wait_for(lambda: _wait_for_pool(host))

    if return_agent:
        return host, keys[0].account(), c
    else:
        return host


def _wait_for_pool(host):
    pools = host.storagePools()
    return len(pools) > 0 and pools[0].state == 'active'


def _is_valid_super_client(client):
    try:
        # stupid test
        identities = client.list_identity()
        return len(identities) == 1 and identities[0].name == 'superadmin'
    except:
        return False


def _get_super_client(request):
    global _SUPER_CLIENT

    if _SUPER_CLIENT is not None:
        return _SUPER_CLIENT

    l = open('/tmp/cattle-api.lock', 'w')
    fcntl.flock(l, fcntl.LOCK_EX)

    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key='superadmin',
                             secret_key='superadminpass')

    if _is_valid_super_client(client):
        _SUPER_CLIENT = client
        if request is not None:
            request.addfinalizer(
                lambda: delete_sim_instances(client))
        fcntl.flock(l, fcntl.LOCK_UN)
        return client

    super_admin = find_one(client.list_account, name='superadmin')
    super_admin = activate_resource(client, super_admin)

    creds = client.list_api_key(_role='superadmin',
                                accountId=super_admin.id)

    cred = None
    for i in creds:
        if i.removed is None:
            cred = i
            break

    if cred is None:
        cred = client.create_api_key(_role='superadmin',
                                     accountId=super_admin.id,
                                     publicValue='superadmin',
                                     secretValue='superadminpass')
        client.wait_success(cred)

    fcntl.flock(l, fcntl.LOCK_UN)

    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key=cred.publicValue,
                             secret_key=cred.secretValue)

    assert _is_valid_super_client(client)
    _SUPER_CLIENT = client

    if request is not None:
        request.addfinalizer(
            lambda: delete_sim_instances(client))

    return client


def activate_resource(client, obj):
    if obj.state == 'inactive':
        obj = client.wait_success(obj.activate())

    return obj


def random_num():
    return random.randint(0, 1000000)


def wait_all_success(client, objs, timeout=DEFAULT_TIMEOUT):
    ret = []
    for obj in objs:
        obj = client.wait_success(obj, timeout)
        ret.append(obj)

    return ret


def wait_for_condition(client, resource, check_function, fail_handler=None,
                       timeout=DEFAULT_TIMEOUT):
    sleep_time = _sleep_time()
    start = time.time()
    resource = client.reload(resource)
    while not check_function(resource):
        if time.time() - start > timeout:
            exceptionMsg = 'Timeout waiting for ' + resource.kind + \
                ' to satisfy condition: ' + \
                inspect.getsource(check_function)
            if (fail_handler):
                exceptionMsg = exceptionMsg + fail_handler(resource)
            raise Exception(exceptionMsg)

        time.sleep(sleep_time.next())
        resource = client.reload(resource)

    return resource


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

    assert obj.removeTimeTS >= obj.removedTS


def assert_restored_fields(obj):
    assert obj.removed is None
    assert obj.removeTime is None


def now():
    return datetime.utcnow()


def format_time(time):
    return (time - timedelta(microseconds=time.microsecond)).isoformat() + 'Z'


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


def get_plain_id(admin_client, obj=None):
    if obj is None:
        obj = admin_client
        admin_client = super_client(None)

    ret = admin_client.list(obj.type, uuid=obj.uuid, _plainId='true')
    assert len(ret) == 1
    return ret[0].id


def get_by_plain_id(super_client, type, id):
    obj = super_client.by_id(type, id, _plainId='true')
    if obj is None:
        return None
    objs = super_client.list(type, uuid=obj.uuid)
    if len(objs) == 0:
        return None
    return objs[0]


def create_and_activate(client, type, **kw):
    obj = client.create(type, **kw)
    obj = client.wait_success(obj)

    if obj.state == 'inactive':
        obj = client.wait_success(obj.activate())

    assert obj.state == 'active'
    return obj


def acc_id(client):
    base_url = client.schema.types['schema'].links['collection']
    r = requests.get(base_url,
                     headers=auth_header_map(client))
    return r.headers['x-api-account-id']


def delete_sim_instances(super_client):
    for account in super_client.list_account():
        if account.removed is not None:
            continue

        if account.name is not None and \
                account.name.startswith('Session {}'.format(os.getpid())):
            account = super_client.wait_success(account)
            try:
                if account.removed is None:
                    if account.state == 'active':
                        account = \
                            super_client.wait_success(account.deactivate())
                    super_client.delete(account)
            except:
                pass


def one(method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == 1
    return ret[0]


def process_instances(admin_client, obj, id=None, type=None):
    if id is None:
        id = obj.id

    if type is None:
        type = obj.baseType

    return admin_client.list_process_instance(resourceType=type, resourceId=id,
                                              sort='startTime')


def auth_check(schema, id, access, props=None):
    type = schema.types[id]
    access_actual = set()

    try:
        if 'GET' in type.collectionMethods:
            access_actual.add('r')
    except AttributeError:
        pass

    try:
        if 'GET' in type.resourceMethods:
            access_actual.add('r')
    except AttributeError:
        pass

    try:
        if 'POST' in type.collectionMethods:
            access_actual.add('c')
    except AttributeError:
        pass

    try:
        if 'DELETE' in type.resourceMethods:
            access_actual.add('d')
    except AttributeError:
        pass

    try:
        if 'PUT' in type.resourceMethods:
            access_actual.add('u')
    except AttributeError:
        pass

    assert access_actual == set(access)

    if props is None:
        return 1

    for i in ['name', 'description']:
        if i not in props and i in type.resourceFields:
            acl = set('r')
            if 'c' in access_actual:
                acl.add('c')
            if 'u' in access_actual:
                acl.add('u')
            props[i] = ''.join(acl)

    for i in ['created', 'removed', 'transitioning', 'transitioningProgress',
              'removeTime', 'transitioningMessage', 'id', 'uuid', 'kind',
              'state']:
        if i not in props and i in type.resourceFields:
            props[i] = 'r'

    prop = set(props.keys())
    prop_actual = set(type.resourceFields.keys())

    assert prop_actual == prop

    for name, field in type.resourceFields.items():
        assert name in props

        prop = set(props[name])
        prop_actual = set('r')

        prop.add(name)
        prop_actual.add(name)

        if field.create and 'c' in access_actual:
            prop_actual.add('c')
        if field.update and 'u' in access_actual:
            prop_actual.add('u')
        if field.readOnCreateOnly:
            prop_actual.add('o')

        assert prop_actual == prop

    return 1


def resource_action_check(schema, id, actions):
    action_keys = set(actions)
    keys_received = set(schema.types[id].resourceActions.keys())
    assert keys_received == action_keys


def _sleep_time():
    sleep = 0.01
    while True:
        yield sleep
        sleep *= 2
        if sleep > 1:
            sleep = 1


def wait_state(client, obj, state):
    def is_state():
        o = client.reload(obj)
        if o.state == state:
            return True
        if state == 'removed' and o.removed is not None:
            return True
        return False
    try:
        wait_for(is_state)
    except:
        obj = client.reload(obj)
        msg = 'Timeout waiting for state {}, resource is {} : {}'.format(
            state, obj.state, obj
        )
        raise Exception(msg)

    return client.reload(obj)


def wait_for(callback, timeout=DEFAULT_TIMEOUT, fail_handler=None):
    sleep_time = _sleep_time()
    start = time.time()
    ret = callback()
    while ret is None or ret is False:
        time.sleep(sleep_time.next())
        if time.time() - start > timeout:
            exception_msg = 'Timeout waiting for condition.'
            if fail_handler:
                exception_msg = exception_msg + ' Fail handler message: ' + \
                                fail_handler()
            raise Exception(exception_msg)
        ret = callback()
    return ret


def find_one(method, *args, **kw):
    return find_count(1, method, *args, **kw)[0]


def find_count(count, method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == count
    return ret


def resource_pool_items(admin_client, obj, type=None, qualifier=None):
    id = get_plain_id(admin_client, obj)

    if type is None:
        type = obj.type

    if qualifier is None:
        return admin_client.list_resource_pool(ownerType=type,
                                               ownerId=id)
    else:
        return admin_client.list_resource_pool(ownerType=type,
                                               ownerId=id,
                                               qualifier=qualifier)


def wait_setting_active(api_client, setting, timeout=45):
    sleep_time = _sleep_time()
    start = time.time()
    setting = api_client.by_id_setting(setting.name)
    while setting.value != setting.activeValue:
        time.sleep(sleep_time.next())
        setting = api_client.by_id('setting', setting.id)
        if time.time() - start > timeout:
            msg = 'Timeout waiting for [{0}] to be done'.format(setting)
            raise Exception(msg)

    return setting


def api_client(access_key, secret_key, project_id=None):
    return cattle.from_env(url=cattle_url(project_id),
                           cache=False,
                           access_key=access_key,
                           secret_key=secret_key)


def base_url():
    base_url = cattle_url()
    if (base_url.endswith('/v2/schemas')):
        base_url = base_url[:-7]
    elif (not base_url.endswith('/v2/')):
        base_url = base_url + '/v2/'
    return base_url


def get_plain_member(member):
    return {
        'role': member.role,
        'externalId': member.externalId,
        'externalIdType': member.externalIdType
    }


def get_plain_members(members):
    plain_members = []
    for member in members.data:
        plain_members.append(get_plain_member(member))
    return plain_members


def auth_header(client):
    return auth_header_from_keys(client._access_key, client._secret_key)


def auth_header_from_keys(access_key, secret_key):
    b = base64.encodestring(access_key + ':' + secret_key)
    return ['Authorization: Basic {}'.format(b.replace('\n', ''))]


def auth_header_map(client):
    b = base64.encodestring(client._access_key + ':' + client._secret_key)
    return {'Authorization': 'Basic {}'.format(b.replace('\n', ''))}


def retry(func, tries=3):
    for i in range(tries):
        try:
            return func()
        except:
            if i == (tries - 1):
                raise
