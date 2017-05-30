from common import *  # NOQA
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


@pytest.fixture(scope='module', autouse=True)
def turn_on_off_local_auth(request, admin_user_client):
    username = random_str()
    password = random_str()
    admin_user_client.create_localAuthConfig(enabled=True,
                                             username=username,
                                             password=password)

    def fin():
        admin_user_client.create_localAuthConfig(enabled=None,
                                                 username=username,
                                                 password=password)
        # Proves auth is off because keys are invalid and would be reject
        assert from_env(access_key='bad_key', secret_key='bad_key2').valid()

    request.addfinalizer(fin)


def make_user_and_client(admin_user_client, name_base='user ',
                         username=None, password=None):
    if username is None:
        username = name_base + random_str()
    if password is None:
        password = 'password ' + random_str()
    account = admin_user_client.create_account(name=name_base + random_str(),
                                               kind="user")
    admin_user_client.wait_success(account)

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

    jwt = token['jwt']
    user = token['userIdentity']
    assert user['login'] == username
    assert token['user'] == username
    start_client._auth = LocalAuth(jwt)
    start_client._access_key = None
    start_client._secret_key = None
    start_client.reload_schema()
    start_client.valid()
    identities = start_client.list_identity()

    assert len(identities) == 1
    assert identities[0].externalId == account.id
    assert identities[0].login == username

    projects = start_client.list_project()
    assert len(projects) == 1
    members = projects[0].projectMembers()
    assert len(members) == 1
    member = get_plain_member(members[0])
    assert member['externalId'] == identities[0].externalId
    assert member['externalIdType'] == identities[0].externalIdType
    assert member['role'] == 'owner'

    return start_client


@pytest.mark.nonparallel
def test_local_login(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()
    projects = client.list_project()
    assert len(projects) == 1

    assert len(identities) == 1
    assert identities[0].externalId == account.id

    client, account, username, password =\
        make_user_and_client(admin_user_client, password="   " + random_str())
    identities = client.list_identity()
    projects = client.list_project()
    assert len(projects) == 1

    assert len(identities) == 1
    assert identities[0].externalId == account.id


@pytest.mark.nonparallel
def test_local_login_only_create1_project(admin_user_client):
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
def test_local_login_change_password(admin_user_client):
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
def test_local_incorrect_login(admin_user_client):
    token = requests.post(base_url() + 'token',
                          {
                              'code': random_str() + ':' + random_str()
    })

    assert token.status_code == 401

    token = token.json()

    assert token['type'] == 'error'
    assert token['status'] == 401


@pytest.mark.nonparallel
def test_local_project_members(admin_user_client):
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
    assert user1_client.by_id('project', project.id) is not None
    assert user2_client.by_id('project', project.id) is not None


def idToMember(identity, role):
    return {
        'externalId': identity.externalId,
        'externalIdType': identity.externalIdType,
        'role': role
    }


@pytest.mark.nonparallel
def test_local_project_create(admin_user_client):
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
def test_search_identity_name(admin_user_client):
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
def test_inactive_active_login_account(admin_user_client, request):
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

    account = admin_user_client.reload(account)
    account.activate()
    admin_user_client.wait_success(account)

    client.reload_schema()
    assert client.list_identity()[0]['login'] == username

    token = requests.post(base_url() + 'token', {
        'code': username + ':' + password
    }).json()

    assert token['user'] == username
    assert token['userIdentity']['login'] == username


@pytest.mark.nonparallel
def test_deleted_account_login(admin_user_client, request):
    client, account, username, password =\
        make_user_and_client(admin_user_client)
    identities = client.list_identity()
    projects = client.list_project()

    assert len(projects) == 1
    assert len(identities) == 1
    assert identities[0].externalId == account.id

    account = admin_user_client.by_id("account", account.id)
    account.deactivate()
    admin_user_client.wait_success(account)
    admin_user_client.delete(account)

    with pytest.raises(ApiError) as e:
        client.list_identity()
    assert e.value.error.status == 401

    token = requests.post(base_url() + 'token', {
        'code': username + ':' + password
    })
    token = token.json()

    assert token['type'] == 'error'
    assert token['status'] == 401

    account = admin_user_client.wait_success(account)
    account.purge()
    admin_user_client.wait_success(account)

    client, account, username, password =\
        make_user_and_client(admin_user_client,
                             username=username,
                             password=password)

    assert client.list_identity()[0]['login'] == username

    token = requests.post(base_url() + 'token', {
        'code': username + ':' + password
    }).json()

    assert token['user'] == username
    assert token['userIdentity']['login'] == username

    new_projects = client.list_project()
    assert len(new_projects) == 1
    assert new_projects[0].id != projects[0].id


@pytest.mark.nonparallel
def test_list_members_inactive_deleted_member(admin_user_client):
    user1_client, account, username, password =\
        make_user_and_client(admin_user_client)

    user2_client, account2, username, password =\
        make_user_and_client(admin_user_client)

    members = get_plain_members(user1_client.list_identity())
    members[0]['role'] = 'owner'
    project = user1_client.create_project(members=members)

    project = user1_client.wait_success(project)

    members = [
        get_plain_member(user1_client.list_identity()[0]),
        get_plain_member(user2_client.list_identity()[0])
    ]
    members[0]['role'] = 'owner'
    members[1]['role'] = 'member'
    project.setmembers(members=members)

    account2 = admin_user_client.by_id("account", account2.id)
    account2.deactivate()
    account2 = admin_user_client.wait_success(account2)
    account2.remove()
    account2 = admin_user_client.wait_success(account2)
    account2.purge()

    admin_user_client.wait_success(account2)

    project = user1_client.by_id("project", project.id)
    assert len(project.projectMembers()) == 1


@pytest.mark.nonparallel
def test_cant_create_multiple_users_same_login(admin_user_client):
    user1_client, account, username, password =\
        make_user_and_client(admin_user_client)

    with pytest.raises(ApiError) as e:
        make_user_and_client(admin_user_client,
                             username=username, password=password)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'publicValue'


@pytest.mark.nonparallel
def test_passwords_non_alpha_numeric_characters(admin_user_client):
    chars = [':', ';', '@', '!', '#', '$', '%', '^', '&', '*', '(', ')',
             '+', '/', '<', '>', '?']
    name = random_str()
    username = random_str()
    account = admin_user_client.create_account(name=name,
                                               kind="user")
    admin_user_client.wait_success(account)
    assert account.name == name

    for char in chars:
        password = 'the{}Ran{}pa22'.format(char, char)
        key = admin_user_client.create_password(publicValue=username,
                                                secretValue=password,
                                                accountId=account.id)
        key = admin_user_client.wait_success(key)
        make_client(admin_user_client, account, username, password)
        admin_user_client.wait_success(key.deactivate())
        admin_user_client.delete(key)
        admin_user_client.wait_success(key)
