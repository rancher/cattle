import common
import pytest


@pytest.fixture(scope='session')
def cattle_url(project_id=None):
    return common.cattle_url(project_id)


@pytest.fixture(scope='function')
def new_context(admin_user_client, request):
    return common.new_context(admin_user_client, request)


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

    client = common.api_client(key.publicValue, key.secretValue)
    init(client)
    return client


@pytest.fixture(scope='session')
def super_client(request):
    return common.super_client(request)


def init(admin_user_client):
    kv = {
        'task.process.replay.schedule': '2',
        'task.config.item.migration.schedule': '5',
        'task.config.item.source.version.sync.schedule': '5',
    }
    for k, v in kv.items():
        admin_user_client.create_setting(name=k, value=v)


@pytest.fixture
def random_str():
    return common.random_str()
