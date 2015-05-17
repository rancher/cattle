import cattle
import os
import pytest
import random
import time
import inspect
from datetime import datetime, timedelta

ACCESS_KEY = 0
SECRET_KEY = 1
ACCOUNT = 2
PROJECT_ACCOUNT = 3
CLIENT = 4
PROJECT_CLIENT = 5

NOT_NONE = object()
DEFAULT_TIMEOUT = 90
DEFAULT_AGENT_URI = 'ssh://root@localhost:22'
DEFAULT_AGENT_UUID = 'test-agent'
SLEEP_DELAY = 0.5
ACCOUNT_LIST = ['admin', 'agent', 'user', 'agentRegister',
                'readAdmin', 'token', 'superadmin', 'service', 'project']
PROJECT_ACCOUNTS = {'admin': True, 'user': True}
_SUPER_CLIENT = None


@pytest.fixture(scope='session')
def cattle_url():
    default_url = 'http://localhost:8080/v1/schemas'
    return os.environ.get('CATTLE_URL', default_url)


def _admin_client():
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key='admin',
                           secret_key='adminpass')


def _client_and_project(access_key, secret_key, create_project):
    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key=access_key,
                             secret_key=secret_key)

    if not create_project:
        return client, client, None

    projects = client.list_project()
    if len(projects):
        project = projects[0]
        return client, client_for_project(project), project
    else:
        project = create_and_activate(client, 'project',
                                      name='project for tests')
        return client, client_for_project(project), project


@pytest.fixture(scope='session')
def project_client(admin_client):
    p = admin_client.list_project(name='test-project')

    if len(p) == 0:
        p = admin_client.create_project(name='test-project')
        p = admin_client.wait_success(p)
    else:
        p = p[0]

    return client_for_project(p)


def client_for_project(project):
    access_key = random_str()
    secret_key = random_str()
    active_cred = None
    client = super_client(None)
    for cred in client.list_api_key(accountId=project.id):
        if cred.state == 'active' and cred.publicValue is not None:
            active_cred = cred
            break

    if active_cred is None:
        active_cred = client.create_api_key({
            'accountId': project.id,
            'publicValue': access_key,
            'secretValue': secret_key
        })

    active_cred = wait_success(client, active_cred)
    if active_cred.state != 'active':
        wait_success(client, active_cred.activate())

    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=active_cred.publicValue,
                           secret_key=active_cred.secretValue)


def create_user(admin_client, user_name, kind=None):
    if kind is None:
        kind = user_name

    password = user_name + 'pass'
    account = create_type_by_uuid(admin_client, 'account', user_name,
                                  kind=kind,
                                  name=user_name)

    active_cred = None
    for cred in account.credentials():
        if cred.kind == 'apiKey' and cred.publicValue == user_name \
                and cred.secretValue == password:
            active_cred = cred
            break

    if active_cred is None:
        active_cred = admin_client.create_api_key({
            'accountId': account.id,
            'publicValue': user_name,
            'secretValue': password
        })

    active_cred = wait_success(admin_client, active_cred)
    if active_cred.state != 'active':
        wait_success(admin_client, active_cred.activate())

    return [user_name, password, account]


def _is_valid_super_client(client):
    try:
        # stupid test
        return 'zone' in client.schema.types
    except:
        return False


@pytest.fixture(scope='session')
def super_client(request):
    global _SUPER_CLIENT

    if _SUPER_CLIENT is not None:
        return _SUPER_CLIENT

    if request is not None:
        request.addfinalizer(
            lambda: delete_sim_instances(client))

    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key='superadmin',
                             secret_key='superadminpass')

    if _is_valid_super_client(client):
        _SUPER_CLIENT = client
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

    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key=cred.publicValue,
                             secret_key=cred.secretValue)

    assert _is_valid_super_client(client)
    _make_not_account_scoped(client, super_admin)
    _SUPER_CLIENT = client
    return client


def _make_not_account_scoped(super_client, account):
    if account is None:
        return None

    # TODO: remove once data in tests is cleaned up
    key = 'io.cattle.platform.allocator.constraint.AccountConstraintsProvider'
    account = super_client.reload(account)
    if account.data is None or key not in account.data:
        data = account.data
        if data is None:
            data = {}
        data[key] = {'accountScoped': False}
        account = super_client.update(account, data=data)
    return account


@pytest.fixture(scope='session')
def accounts(super_client):
    result = {}
    for user_name in ACCOUNT_LIST:
        access_key, secret_key, account = create_user(super_client,
                                                      user_name,
                                                      kind=user_name)
        client, project_client, project = \
            _client_and_project(access_key,
                                secret_key,
                                user_name in PROJECT_ACCOUNTS)

        if project_client is None:
            project_client = client

        if project is None:
            project = account

        account = _make_not_account_scoped(super_client, account)
        project = _make_not_account_scoped(super_client, project)
        result[user_name] = [access_key,
                             secret_key,
                             account,
                             project,
                             client,
                             project_client]

    system_account = super_client.list_account(kind='system', uuid='system')[0]
    result['system'] = [None, None, system_account, system_account, None, None]

    return result


@pytest.fixture(scope='session')
def clients(accounts):
    clients = {}
    for account in ACCOUNT_LIST:
        clients[account] = accounts[account][CLIENT]
    return clients


@pytest.fixture(scope='session')
def system_account(accounts):
    return accounts['system'][PROJECT_ACCOUNT]


@pytest.fixture(scope='session')
def admin_account(accounts):
    return accounts['admin'][PROJECT_ACCOUNT]


@pytest.fixture(scope='session')
def user_account(accounts):
    return accounts['user'][PROJECT_ACCOUNT]


@pytest.fixture(scope='session')
def token_account(accounts):
    return accounts['token'][PROJECT_ACCOUNT]


@pytest.fixture(scope='session')
def super_account(accounts):
    return accounts['superadmin'][PROJECT_ACCOUNT]


@pytest.fixture(scope='session')
def client(accounts):
    return accounts['user'][PROJECT_CLIENT]


@pytest.fixture(scope='session')
def user_client(accounts):
    return accounts['user'][CLIENT]


@pytest.fixture(scope='session')
def admin_client(accounts):
    return accounts['admin'][PROJECT_CLIENT]


@pytest.fixture(scope='session')
def admin_user_client(accounts):
    return accounts['admin'][CLIENT]


@pytest.fixture(scope='session')
def token_client(accounts):
    return accounts['token'][CLIENT]


@pytest.fixture(scope='session')
def agent_client(accounts):
    return accounts['agent'][CLIENT]


@pytest.fixture(scope='session')
def service_client(accounts):
    return accounts['service'][CLIENT]


def create_sim_context(super_client, uuid, ip=None, account=None,
                       public=False):
    context = kind_context(super_client,
                           'sim',
                           external_pool=True,
                           account=account,
                           uri='sim://' + uuid,
                           uuid=uuid,
                           host_public=public)
    context['imageUuid'] = 'sim:{}'.format(random_num())

    host = context['host']

    if len(host.ipAddresses()) == 0 and ip is not None:
        ip = create_and_activate(super_client, 'ipAddress',
                                 address=ip,
                                 isPublic=public)
        map = super_client.create_host_ip_address_map(hostId=host.id,
                                                      ipAddressId=ip.id)
        map = super_client.wait_success(map)
        assert map.state == 'active'

    if len(host.ipAddresses()):
        context['hostIp'] = host.ipAddresses()[0]

    return context


@pytest.fixture(scope='session')
def sim_context(request, super_client):
    context = create_sim_context(super_client, 'simagent1', ip='192.168.10.10',
                                 public=True)

    return context


@pytest.fixture(scope='session')
def sim_context2(super_client):
    return create_sim_context(super_client, 'simagent2', ip='192.168.10.11',
                              public=True)


@pytest.fixture(scope='session')
def sim_context3(super_client):
    return create_sim_context(super_client, 'simagent3', ip='192.168.10.12',
                              public=True)


@pytest.fixture
def new_sim_context(super_client):
    uri = 'sim://' + random_str()
    sim_context = kind_context(super_client, 'sim', uri=uri, uuid=uri)
    sim_context['imageUuid'] = 'sim:{}'.format(random_num())

    for i in ['host', 'pool', 'agent']:
        sim_context[i] = super_client.wait_success(sim_context[i])

    host = sim_context['host']
    pool = sim_context['pool']
    agent = sim_context['agent']

    assert host is not None
    assert pool is not None
    assert agent is not None

    return sim_context


@pytest.fixture(scope='session')
def user_sim_context(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent', ip='192.168.11.1',
                              account=user_account)


@pytest.fixture(scope='session')
def user_sim_context2(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent2', ip='192.168.11.2',
                              account=user_account)


@pytest.fixture(scope='session')
def user_sim_context3(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent3', ip='192.168.11.3',
                              account=user_account)


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


@pytest.fixture
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


@pytest.fixture
def wait_for_condition(client, resource, check_function, fail_handler=None,
                       timeout=DEFAULT_TIMEOUT):
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

        time.sleep(.5)
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

    assert obj.removeTimeTS > obj.removedTS


def assert_restored_fields(obj):
    assert obj.removed is None
    assert obj.removeTime is None


def now():
    return datetime.utcnow()


def format_time(time):
    return (time - timedelta(microseconds=time.microsecond)).isoformat() + 'Z'


def get_agent(admin_client, name, default_uri=DEFAULT_AGENT_URI,
              default_agent_uuid=DEFAULT_AGENT_UUID, account=None):
    name = name.upper()
    uri_name = '{0}_URI'.format(name.upper())
    uuid_name = '{0}_AGENT_UUID'.format(name.upper())

    uri = os.getenv(uri_name, default_uri)
    uuid = os.getenv(uuid_name, default_agent_uuid)

    data = {}
    if account is not None:
        account_id = get_plain_id(admin_client, account)
        data['agentResourcesAccountId'] = account_id

    agent = create_type_by_uuid(admin_client, 'agent', uuid, validate=False,
                                uri=uri, data=data)

    if account is not None:
        assert agent.data.agentResourcesAccountId == account_id

    while len(agent.hosts()) == 0:
        time.sleep(SLEEP_DELAY)

    return agent


def kind_context(admin_client, kind, external_pool=False,
                 uri=DEFAULT_AGENT_URI,
                 uuid=DEFAULT_AGENT_UUID,
                 host_public=False,
                 agent=None,
                 account=None):
    if agent is None:
        kind_agent = get_agent(admin_client, kind, default_agent_uuid=uuid,
                               default_uri=uri, account=account)
    else:
        kind_agent = agent

    hosts = filter(lambda x: x.kind == kind and x.removed is None,
                   kind_agent.hosts())
    assert len(hosts) == 1
    kind_host = activate_resource(admin_client, hosts[0])

    if kind_host.isPublic != host_public:
        kind_host = admin_client.update(kind_host, isPublic=host_public)

    assert kind_host.isPublic == host_public
    assert kind_host.accountId == kind_agent.accountId or \
        get_plain_id(admin_client, kind_host.account()) == \
        str(kind_agent.data.agentResourcesAccountId)

    pools = kind_host.storagePools()
    assert len(pools) == 1
    kind_pool = activate_resource(admin_client, pools[0])

    assert kind_pool.accountId == kind_agent.accountId or \
        get_plain_id(admin_client, kind_pool.account()) == \
        str(kind_agent.data.agentResourcesAccountId)

    context = {
        'host': kind_host,
        'pool': kind_pool,
        'agent': kind_agent
    }

    if external_pool:
        pools = admin_client.list_storagePool(kind=kind, external=True)
        assert len(pools) == 1
        context['external_pool'] = activate_resource(admin_client, pools[0])

        assert pools[0].accountId is not None

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
    obj = client.list_api_key()[0]
    return obj.account().id


def delete_sim_instances(admin_client):
    if admin_client is None:
        return

    to_delete = []
    to_delete.extend(admin_client.list_instance(state='running', limit=1000))
    to_delete.extend(admin_client.list_instance(state='starting', limit=1000))
    to_delete.extend(admin_client.list_instance(state='stopped', limit=1000))

    for c in to_delete:
        hosts = c.hosts()
        if len(hosts) and hosts[0].kind == 'sim':
            nsps = c.networkServiceProviders()
            if len(nsps) > 0 and nsps[0].uuid == 'nsp-test-nsp':
                continue

            try:
                admin_client.delete(c)
            except:
                pass

    for state in ['active', 'reconnecting']:
        for a in admin_client.list_agent(state=state, include='instances',
                                         uri_like='delegate%'):
            if not callable(a.instances):
                for i in a.instances:
                    try:
                        if i.state != 'running' and len(i.hosts()) > 0 and \
                                i.hosts()[0].agent().uri.startswith('sim://'):
                            a.deactivate()
                            break
                    except:
                        pass


def one(method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == 1
    return ret[0]


def process_instances(admin_client, obj, id=None, type=None):
    if id is None:
        id = get_plain_id(admin_client, obj)

    if type is None:
        type = obj.type

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

        assert prop_actual == prop

    return 1


def resource_action_check(schema, id, actions):
    action_keys = set(actions)
    keys_received = set(schema.types[id].resourceActions.keys())
    assert keys_received == action_keys


def wait_for(callback, timeout=DEFAULT_TIMEOUT):
    start = time.time()
    ret = callback()
    while ret is None or ret is False:
        time.sleep(.5)
        if time.time() - start > timeout:
            raise Exception('Timeout waiting for condition')
        ret = callback()
    return ret


def find_one(method, *args, **kw):
    return find_count(1, method, *args, **kw)[0]


def find_count(count, method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == count
    return ret


def create_sim_container(admin_client, sim_context, *args, **kw):
    c = admin_client.create_container(*args,
                                      imageUuid=sim_context['imageUuid'],
                                      **kw)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    return c


def create_agent_instance_nsp(admin_client, sim_context):
    accountId = sim_context['host'].accountId
    network = create_and_activate(admin_client, 'hostOnlyNetwork',
                                  isPublic=True,
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True,
                                  accountId=accountId)

    create_and_activate(admin_client, 'subnet',
                        networkAddress='192.168.0.0',
                        networkId=network.id,
                        accountId=accountId)

    return create_and_activate(admin_client, 'agentInstanceProvider',
                               networkId=network.id,
                               agentInstanceImageUuid=sim_context['imageUuid'],
                               accountId=accountId)


@pytest.fixture(scope='session')
def test_network(super_client, sim_context):
    network = create_type_by_uuid(super_client, 'hostOnlyNetwork',
                                  'nsp-test-network',
                                  isPublic=True,
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True)

    create_type_by_uuid(super_client, 'subnet',
                        'nsp-test-subnet',
                        networkAddress='192.168.0.0',
                        networkId=network.id)

    nsp = create_type_by_uuid(super_client, 'agentInstanceProvider',
                              'nsp-test-nsp',
                              networkId=network.id,
                              agentInstanceImageUuid='sim:test-nsp')

    create_type_by_uuid(super_client, 'portService',
                        'nsp-test-port-service',
                        networkId=network.id,
                        networkServiceProviderId=nsp.id)

    for i in nsp.instances():
        i = super_client.wait_success(i)
        if i.state != 'running':
            super_client.wait_success(i.start())

        agent = super_client.wait_success(i.agent())
        if agent.state != 'active':
            super_client.wait_success(agent.activate())

    return network


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


@pytest.fixture(scope='session')
def network(super_client):
    network = create_type_by_uuid(super_client, 'network', 'test_vm_network',
                                  isPublic=True)

    subnet = create_type_by_uuid(super_client, 'subnet', 'test_vm_subnet',
                                 isPublic=True,
                                 networkId=network.id,
                                 networkAddress='192.168.0.0',
                                 cidrSize=24)

    vnet = create_type_by_uuid(super_client, 'vnet', 'test_vm_vnet',
                               networkId=network.id,
                               uri='fake://')

    create_type_by_uuid(super_client, 'subnetVnetMap', 'test_vm_vnet_map',
                        subnetId=subnet.id,
                        vnetId=vnet.id)

    return network


@pytest.fixture(scope='session')
def subnet(admin_client, network):
    subnets = network.subnets()
    assert len(subnets) == 1
    return subnets[0]


@pytest.fixture(scope='session')
def vnet(admin_client, subnet):
    vnets = subnet.vnets()
    assert len(vnets) == 1
    return vnets[0]


def wait_setting_active(api_client, setting, timeout=45):
    start = time.time()
    setting = api_client.by_id_setting(setting.name)
    while setting.value != setting.activeValue:
        time.sleep(.5)
        setting = api_client.by_id('setting', setting.id)
        if time.time() - start > timeout:
            msg = 'Timeout waiting for [{0}] to be done'.format(setting)
            raise Exception(msg)

    return setting


def create_container(client, sim_context, **kw):
    args = {
        'imageUuid': sim_context['imageUuid'],
        'requestedHostId': sim_context['host'].id,
        }
    args.update(kw)

    return client.create_container(**args)


@pytest.fixture(scope='session')
def context(admin_user_client, super_client):
    account = admin_user_client.create_account(kind='project')
    keys = admin_user_client.create_api_key(accountId=account.id)
    assert keys.accountId == account.id

    account = admin_user_client.wait_success(account)
    assert account.state == 'active'

    keys = admin_user_client.wait_success(keys)
    assert keys.state == 'active'

    client = cattle.from_env(url=cattle_url(),
                             cache=False,
                             access_key=keys.publicValue,
                             secret_key=keys.secretValue)

    account = client.reload(account)

    uri = 'sim://' + random_str()
    sim_context = kind_context(super_client, 'sim', uri=uri, uuid=uri,
                               account=account)
    sim_context['imageUuid'] = 'sim:{}'.format(random_num())

    for i in ['host', 'pool', 'agent']:
        obj = sim_context[i]
        assert obj is not None
        sim_context[i] = super_client.wait_success(obj)

    sim_context['client'] = client
    sim_context['project'] = account

    nsp = super_client.list_network_service_provider(accountId=account.id)[0]
    super_client.update(nsp, agentInstanceImageUuid='sim:test-nsp')

    return sim_context
