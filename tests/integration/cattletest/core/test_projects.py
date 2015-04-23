from common_fixtures import *  # NOQA
from common_fixtures import _client_for_user
from common_fixtures import _admin_client
from gdapi import ApiError


ACCOUNT_LIST = [
    "Owner",
    "Member",
    "Stranger",
    "OutThereUser"
]


PROJECTS = set([])


def make_accounts():
    result = {}
    admin_client = _admin_client()
    for user_name in ACCOUNT_LIST:
        result[user_name] = create_user(admin_client,
                                        user_name,
                                        kind="user")
    return result


@pytest.fixture(autouse=True, scope="session")
def clean_up_projects(admin_client, request):
    def fin():
        for project in PROJECTS:
            try:
                admin_client.delete(admin_client.by_id('project', project))
            except ApiError as e:
                assert e.error.status == 404
        assert len(get_ids(admin_client.list_project()) & PROJECTS) == 0
    request.addfinalizer(fin)
    pass


@pytest.fixture()
def project(project_clients, admin_client, request):
    project = _create_project(project_clients, 'Owner')

    def fin():
        try:
            admin_client.delete(admin_client.by_id('project', project))
        except ApiError as e:
            assert e.error.status == 404

    request.addfinalizer(fin)
    return project


def _set_members(client, id, members, status):
    project = client.by_id('project', id)
    if status is None:
        got_members = project.setmembers(members=members)
        for member in got_members.data:
            _admin_client().wait_success(member)
        got_members = get_plain_members(project.projectMembers())
        assert len(got_members) == len(members)
        diff_members(members, got_members)
    else:
        with pytest.raises(ApiError) as e:
            project.setmembers(members=members)
        assert e.value.error.status == status


def _create_project(project_clients, user):
    client = project_clients[user]
    members = _create_members(project_clients, [user])
    project = client.create_project(members=members)
    project = _admin_client().wait_success(project)
    project = client.by_id('project', project.id)
    got_members = get_plain_members(project.projectMembers())
    diff_members(members, got_members)
    PROJECTS.add(project.id)
    assert project.id == project.id
    return project


def get_plain_members(members):
    plain_members = []
    for member in members.data:
        plain_members.append({
            'role': member.role,
            'externalId': member.externalId,
            'externalIdType': member.externalIdType
        })
    return plain_members


def get_ids(items):
    ids = []
    for item in items:
        ids.append(item.id)
    return set(ids)


def _get_members(client, id, actual_members):
    project = client.by_id('project', id)
    assert len(project.projectMembers()) == len(actual_members)


def _create_project_with_members(client, members):
    project = client.create_project(members=members)
    project = _admin_client().wait_success(project)
    project = client.by_id('project', project.id)
    PROJECTS.add(project.id)
    got_members = get_plain_members(project.projectMembers())
    assert len(members) == len(got_members)
    diff_members(members, got_members)
    return project


def diff_members(members, got_members):
    assert len(members) == len(got_members)
    members_a = set([])
    members_b = set([])
    for member in members:
        members_a.add(member['externalId'] + '  ' + member['externalIdType']
                      + '  ' + member['role'])
    for member in got_members:
        members_b.add(member['externalId'] + '  ' + member['externalIdType']
                      + '  ' + member['role'])
    assert members_a == members_b


def _create_members(project_clients, members):
    newMembers = []
    for member in members:
        newMembers.append({
            'role': 'owner' if member == 'Owner' else 'member',
            'externalId': acc_id(project_clients[member]),
            'externalIdType': 'rancher_id'
        })
    return newMembers


def all_owners(members):
    for member in members:
        member['role'] = 'owner'
    return members


@pytest.fixture(scope='session')
def project_clients(admin_client):
    accounts = make_accounts()
    clients = {}
    for account in ACCOUNT_LIST:
        clients[account] = _client_for_user(account, accounts)
    clients['admin'] = admin_client
    return clients


def acc_id(client):
    obj = client.list_api_key()[0]
    return obj.account().id


@pytest.fixture()
def members(project_clients):
    members = ['Owner', 'Member']
    return _create_members(project_clients, members)


def test_update_project(project_clients, project):
    project_clients['Owner'].update(
        project, name='Project Name', description='Some description')
    assert project_clients['Owner'].by_id(
        'project', project.id).name == 'Project Name'
    assert project_clients['Owner'].by_id(
        'project', project.id).description == 'Some description'
    with pytest.raises(ApiError) as e:
        project_clients['Member'].update(
            project, name='Project Name from Member', description='Loop hole?')
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        project_clients['Stranger'].update(
            project, name='Project Name from Stranger', description='Changed')
    assert e.value.error.status == 404


def test_set_members(project_clients, project):
    members = get_plain_members(project.projectMembers())
    members.append({
        'role': 'member',
        'externalId': acc_id(project_clients['Member']),
        'externalIdType': 'rancher_id'
    })
    _set_members(project_clients['Owner'], project.id, None, 422)
    _set_members(project_clients['Owner'], project.id, [], 422)
    _set_members(project_clients['Owner'], project.id, members, None)
    _set_members(project_clients['Member'], project.id, None, 422)
    _set_members(project_clients['Member'], project.id, [], 422)
    _set_members(project_clients['Member'], project.id, members, 403)
    with pytest.raises(ApiError) as e:
        _set_members(project_clients['Stranger'], project.id, None, 422)
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        _set_members(project_clients['Stranger'], project.id, [], 422)
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        _set_members(project_clients['Stranger'], project.id, members, 403)
    assert e.value.error.status == 404


def test_get_members(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'], members)
    members = project.projectMembers()
    _get_members(project_clients['Owner'], project.id, members)
    _get_members(project_clients['Member'], project.id, members)
    _get_members(project_clients['admin'], project.id, members)
    with pytest.raises(ApiError) as e:
        _get_members(project_clients['Stranger'], project.id, members)
    assert e.value.error.status == 404


def test_list_all_projects():
    projects = _admin_client().list_project()
    projectAccounts = _admin_client().list_account(kind='project', limit=4000)
    ids = []
    ids_2 = []
    for project in projects:
        ids.append(project.id)
    for project in projectAccounts:
        ids_2.append(project.id)
    assert len(list(set(ids) - set(ids_2))) == 0


def _create_resources(super_client, user_account, client):
    create_sim_context(super_client, 'simagent' + random_str(),
                       account=user_account, public=True)
    for x in range(0, 4):
        uuid = "sim:{}".format(random_num())
        client.wait_success(client.create_container(imageUuid=uuid))
    registry = client.create_registry(serverAddress='quay.io',
                                      name='Quay')
    registry = client.wait_success(registry)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        email='test@rancher.com',
        publicValue='rancher',
        secretValue='rancher')
    client.wait_success(reg_cred)


def check_state(client, project_id, states, excludes):
    for type in client.schema.types:
        try:
            for resource in client.list(type, accountId=project_id):
                if resource.type not in excludes:
                    assert resource.state in states
                    assert resource.removed is not None
        except AttributeError:
            pass


def test_delete_project(project_clients, members, super_client):
    project = _create_project_with_members(project_clients['Owner'],
                                           members=members)
    proj_id = project.id
    client = client_for_project(project)
    _create_resources(super_client, project, client)
    assert len(client.list_projectMember()) == 2
    project = super_client.wait_success(project.deactivate())
    project = super_client.wait_success(project.remove())
    check_state(project_clients['Owner'], proj_id,
                ['removed'], ['account', 'project', 'host'])
    super_client.wait_success(project.purge())
    project = project_clients['Owner'].by_id('project', id=proj_id)
    assert project.state == 'purged'
    check_state(project_clients['Owner'], proj_id,
                ['purged', 'removed'], ['account', 'project'])
    project_members = project_clients['admin']\
        .list('projectMember')
    for member in project_members:
        assert member.projectId != proj_id


def test_create_project_no_members(project_clients):
    for client in project_clients.items():
        _create_project(project_clients, client[0])


def test_delete_members(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'], members)
    members = [members[0]]
    assert len(project_clients['Member']
               .by_id('project', project.id).projectMembers()) == 2
    project.setmembers(members=members)
    project = project_clients['Owner'].by_id('project', project.id)
    assert len(project.projectMembers()) == 1
    with pytest.raises(ApiError) as e:
        project_clients['Member'].by_id('project', project.id)
    assert e.value.error.status == 404


def test_change_roles(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'], members)
    assert len(project.projectMembers()) == 2
    new_members = all_owners(get_plain_members(project.projectMembers()))
    project_from_member = project_clients['Member'].by_id('project',
                                                          project.id)
    with pytest.raises(ApiError) as e:
        project_from_member.setmembers(members=new_members)
    assert e.value.error.status == 403
    project.setmembers(members=new_members)
    project_from_member.setmembers(members=new_members)
    project_members_after = get_plain_members(project.projectMembers())
    project_from_member_members_after = get_plain_members(
        project_from_member.projectMembers())
    for member in project_members_after:
        assert member['role'] == 'owner'
    for member in project_from_member_members_after:
        assert member['role'] == 'owner'


def test_delete_other_owners(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'],
                                           all_owners(members))
    project.setmembers(members=members)
    project = project_clients['Member'].by_id('project', project.id)
    new_members = get_plain_members(project.projectMembers())
    for member in new_members:
        if((member['role'] == 'owner') & (member['externalId'] != acc_id(
                project_clients['Member']))):
            new_members.remove(member)
    project.setmembers(members=new_members)
    assert len(project.projectMembers()) == 1
    with pytest.raises(ApiError) as e:
        project_clients['Owner'].by_id('project', project.id)
    assert e.value.error.status == 404
    project = client_for_project(project).list_project()[0]
    got_members = project.projectMembers()
    assert len(got_members) == 1


def test_multiple_owners_add_members(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'],
                                           all_owners(members))
    current_members = get_plain_members(project.projectMembers())
    current_members.append({
        'role': 'member',
        'externalId': acc_id(project_clients['Stranger']),
        'externalIdType': 'rancher_id'
    })
    _set_members(project_clients['Owner'], project.id, current_members, None)
    _set_members(project_clients['Stranger'], project.id, current_members, 403)
    project = project_clients['Stranger'].by_id('project', project.id)
    assert len(project.projectMembers()) == 3
    _set_members(project_clients['Member'], project.id, members, None)
    with pytest.raises(ApiError) as e:
        project.projectMembers()
    assert e.value.error.status == 404
    _set_members(project_clients['Member'], project.id, current_members, None)
    assert len(project.projectMembers()) == len(current_members)


def test_members_cant_delete(project_clients, members):
    project = _create_project_with_members(project_clients['Owner'], members)
    project = project_clients['Member'].by_id("project", project.id)
    got_members = get_plain_members(project.projectMembers())
    id = acc_id(project_clients['Member'])
    for member in got_members:
        if member['externalId'] == id:
            assert member['role'] == 'member'
    with pytest.raises(ApiError) as e:
        project_clients['Member'].delete(project)
    assert e.value.error.status == 403
    _set_members(project_clients['Member'], project.id, [{
        'externalId': acc_id(project_clients['Member']),
        'externalIdType': 'rancher_id',
        'role': 'owner'
    }], 403)


def test_inactive_project(project_clients, members, project):
    project_clients['admin'].wait_success(project.deactivate())
    state = project_clients['admin'].by_id('project', project.id).state
    assert state == 'inactive'
    with pytest.raises(ApiError) as e:
        project.setmembers(members=members)
    assert e.value.error.status == 404
    project = project_clients['admin'].by_id('project', project.id)
    with pytest.raises(ApiError) as e:
        project.setmembers(members=members)
    assert e.value.error.status == 404


def test_project_cant_create_project(project_clients, members, project):
    uuid = project.uuid
    client = client_for_project(project)
    with pytest.raises(ApiError) as e:
        project = client.create_project()
    assert e.value.error.status == 403
    got_project = client.list_project()[0]
    assert got_project.uuid == uuid
    assert len(project.projectMembers()) == len(got_project.projectMembers())
    pass


def test_accessible_projects(project_clients):
    members = _create_members(project_clients, ['OutThereUser'])
    projects = []
    for i in range(0, 4):
        projects.append(_create_project_with_members(project_clients['admin'],
                                                     members))
    assert len(projects) == 4
    got_projects = project_clients['OutThereUser'].list_project()
    project_ids = get_ids(projects)
    got_project_ids = get_ids(got_projects)
    assert len(got_project_ids.intersection(project_ids)) == 4
    for project in projects:
        project.setmembers(members=_create_members(project_clients, ['Owner']))
    got_projects = project_clients['OutThereUser'].list_project()
    project_ids = get_ids(projects)
    got_project_ids = get_ids(got_projects)
    assert len(got_project_ids.intersection(project_ids)) == 0


def test_create_project_no_owner(project_clients):
    project = project_clients['admin'].create_project()
    project = project_clients['admin'].wait_success(project)
    PROJECTS.add(project.id)
    assert len(project.projectMembers()) == 0


def test_list_projects_flag(project_clients):
    admin_client = _admin_client()
    projects = admin_client.list('project')
    ids = set([])
    for project in projects:
        ids.add(project.id)
    projects_with_flag = admin_client.list('project', all='true')
    admin_id = acc_id(admin_client)
    assert len(projects) != len(projects_with_flag)
    for project in projects:
        include = False
        for member in get_plain_members(project.projectMembers()):
            if (member['externalIdType'] == 'rancher_id'):
                if (member['externalId'] == admin_id):
                    include = True
        if (include):
            assert project.id in ids
        else:
            assert project.id not in ids


def test_get_project_not_mine(project_clients, project):
    with pytest.raises(ApiError) as e:
        project_clients['Member'].by_id('project', project.id)
    assert e.value.error.status == 404


def test_set_default_project(project_clients, admin_client, project):
    project.setasdefault()
    assert admin_client.by_id('account', acc_id(project_clients['Owner']))\
        .projectId == project.id
    project = _create_project(project_clients, 'Owner')
    project.setasdefault()
    assert admin_client.by_id('account', acc_id(project_clients['Owner']))\
        .projectId == project.id
