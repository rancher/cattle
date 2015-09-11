from common_fixtures import *  # NOQA
from gdapi import ApiError
from requests.auth import AuthBase
import requests
from cattle import from_env


class LocalAuth(AuthBase):
    def __init__(self, jwt, prj_id=None):
        # setup any auth-related data here
        self.jwt = jwt
        self.prj_id = prj_id

    def __call__(self, r):
        # modify and return the request
        r.headers['Authorization'] = 'Bearer ' + self.jwt
        if self.prj_id is not None:
            r.headers['X-API-Project-Id'] = self.prj_id
        return r


@pytest.fixture(scope='session', autouse=True)
def turn_on_off_local_auth(request, admin_user_client):
    username = os.environ.get('CATTLE_ACCESS_KEY', 'admin')
    password = os.environ.get('CATTLE_SECRET_KEY', 'adminpass')
    admin_user_client.create_localAuthConfig(enabled=True,
                                             username=username,
                                             password=password)

    def fin():
        admin_user_client.create_localAuthConfig(enabled=False,
                                                 username=username,
                                                 password=password)

    request.addfinalizer(fin)


def make_user_and_client(admin_user_client, name_base='user '):
    account = admin_user_client.create_account(name=name_base + random_str(),
                                               kind="user")
    admin_user_client.wait_success(account)

    username = name_base + random_str()
    password = 'password ' + random_str()
    login = admin_user_client.create_password(publicValue=username,
                                              secretValue=password,
                                              accountId=account.id)
    admin_user_client.wait_success(login)

    start_client = make_client(admin_user_client, account, username, password)

    return start_client, account, username, password


def make_client(admin_user_client, account, username, password):
    key = admin_user_client.create_apiKey()
    admin_user_client.wait_success(key)
    start_client = from_env(url=cattle_url(),
                            access_key=key.publicValue,
                            secret_key=key.secretValue)

    token = requests.post(base_url() + 'token', {
        'code': username + ':' + password
    })

    token = token.json()

    assert token['type'] != 'error'

    token = token['jwt']
    start_client._auth = LocalAuth(token)
    start_client.valid()
    identities = start_client.list_identity()

    assert len(identities) == 1
    assert identities[0].externalId == account.id
    return start_client


@pytest.mark.nonparallel
def test_local_login(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()
    projects = client.list_project()
    assert len(projects) != 0

    assert len(identities) == 1
    assert identities[0].externalId == account.id


@pytest.mark.nonparallel
def test_local_login_only_create1_project(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()
    projects = client.list_project()
    original_projects = len(projects)

    assert original_projects != 0
    assert len(identities) == 1
    assert identities[0].externalId == account.id

    client = make_client(admin_user_client, account, username, password)

    identities = client.list_identity()
    projects = client.list_project()
    assert len(projects) == original_projects

    assert len(identities) == 1
    assert identities[0].externalId == account.id


@pytest.mark.nonparallel
def test_local_login_change_password(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)

    credential = client.list_password()

    assert len(credential) == 1
    assert credential[0].publicValue == username

    newPass = random_str()
    credential[0].changesecret(oldSecret=password, newSecret=newPass)
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()

    assert len(identities) == 1
    assert identities[0].externalId == account.id


@pytest.mark.nonparallel
def test_local_incorrect_login(admin_user_client, request):
    token = requests.post(base_url() + 'token',
                          {
                              'code': random_str() + ':' + random_str()
    })

    assert token.status_code == 401

    token = token.json()

    assert token['type'] == 'error'
    assert token['status'] == 401


@pytest.mark.nonparallel
def test_local_project_members(admin_user_client, request):
    user1_client, account, username, password =\
        make_user_and_client(admin_user_client)

    user1_identity = None
    for obj in user1_client.list_identity():
        if obj.externalIdType == 'rancher_id':
            user1_identity = obj
            break

    user2_client, account, username, password =\
        make_user_and_client(admin_user_client)

    user2_identity = None
    for obj in user2_client.list_identity():
        if obj.externalIdType == 'rancher_id':
            user2_identity = obj
            break

    project = user1_client.create_project(members=[
        idToMember(user1_identity, 'owner'),
        idToMember(user2_identity, 'member')
    ])
    admin_user_client.wait_success(project)
    user1_client.by_id('project', project.id)
    user2_client.by_id('project', project.id)


def idToMember(identity, role):
    return {
        'externalId': identity.externalId,
        'externalIdType': identity.externalIdType,
        'role': role
    }


@pytest.mark.nonparallel
def test_local_project_create(admin_user_client, request):
    user1_client, account, username, password =\
        make_user_and_client(admin_user_client)

    identity = None
    for obj in user1_client.list_identity():
        if obj.externalIdType == 'rancher_id':
            identity = obj
            break

    members = [idToMember(identity, 'owner')]
    project = user1_client.create_project(members=members)
    project = user1_client.wait_success(project)

    assert project is not None

    user1_client.delete(project)


@pytest.mark.nonparallel
def test_get_correct_identity(admin_user_client):
    name = "Identity User"
    context = create_context(admin_user_client, name=name)
    identities = context.user_client.list_identity()

    assert len(identities) == 1
    assert identities[0].name == name


@pytest.mark.nonparallel
def test_search_identity_name(admin_user_client, request):
    usernames = []

    for x in range(0, 5):
        client, account, username, password =\
            make_user_and_client(admin_user_client)
        usernames.append(username)

    user_client = create_context(admin_user_client).user_client
    for username in usernames:
        ids = user_client\
            .list_identity(name=username)

        assert len(ids) == 1
        assert ids[0].login == username

        identity = user_client.by_id('identity', id=ids[0].id)

        assert identity.name == ids[0].name
        assert identity.id == ids[0].id
        assert identity.externalId == ids[0].externalId
        assert identity.externalIdType == ids[0].externalIdType


@pytest.mark.nonparallel
def test_search_identity_name_like(admin_user_client, request):
    name_base = random_str()
    usernames = []

    for x in range(0, 5):
        client, account, username, password =\
            make_user_and_client(admin_user_client,
                                 name_base=name_base)
        usernames.append(username)

    identities = admin_user_client.list_identity(all=name_base)

    assert len(identities) == 5
    assert len(usernames) == 5

    found = 0

    for identity in identities:
        for username in usernames:
            if (identity.login == username):
                found += 1

    assert found == 5


@pytest.mark.nonparallel
def test_cant_login_inactive_account(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()
    projects = client.list_project()

    assert len(projects) != 0
    assert len(identities) == 1
    assert identities[0].externalId == account.id

    account = admin_user_client.by_id("account", account.id)
    account.deactivate()
    admin_user_client.wait_success(account)

    with pytest.raises(ApiError) as e:
        client.list_identity()
    assert e.value.error.status == 401

    token = requests.post(base_url() + 'token', {
        'code': username + ':' + password
    })
    token = token.json()

    assert token['type'] == 'error'
    assert token['status'] == 401
