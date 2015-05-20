from common_fixtures import *  # NOQA
from gdapi import ApiError


_USER_LIST = [
    "Owner",
    "Member",
    "Stranger",
    "OutThereUser"
]


PROJECTS = set([])

@pytest.fixture(autouse=True, scope="session")
def clean_up_projects(super_client, request):
    # This randomly times out, don't know why, disabling it
    # on = super_client.create_setting(name='api.projects.use.rancher_id',
    #                                  value='true')
    # wait_setting_active(super_client, on)

    def fin():
        for project in PROJECTS:
            try:
                super_client.delete(super_client.by_id('project', project))
            except ApiError as e:
                assert e.error.status == 404
        assert len(get_ids(super_client.list_project()) & PROJECTS) == 0
        # super_client.delete(on)
    request.addfinalizer(fin)
    pass


@pytest.fixture()
def project(user_clients, admin_user_client, request):
    project = _create_project(admin_user_client, user_clients, 'Owner')

    def fin():
        try:
            admin_user_client.delete(admin_user_client.by_id('project',
                                                             project))
        except ApiError as e:
            assert e.error.status == 404

    request.addfinalizer(fin)
    return project


def _set_members(admin_user_client, client, id, members, status):
    project = client.by_id('project', id)
    if status is None:
        got_members = project.setmembers(members=members)
        for member in got_members.data:
            admin_user_client.wait_success(member)
        got_members = get_plain_members(project.projectMembers())
        assert len(got_members) == len(members)
        diff_members(members, got_members)
    elif (status == 'Attribute'):
        with pytest.raises(AttributeError) as e:
            project.setmembers(members=members)
        assert 'setmembers' in e.value.message
    else:
        with pytest.raises(ApiError) as e:
            project.setmembers(members=members)
        assert e.value.error.status == status


def _get_plain_members(client, project):
    members = client.list_project_member(projectId=project.id)
    return get_plain_members(members)


def _create_project(admin_user_client, user_clients, user):
    client = user_clients[user]
    members = _create_members(user_clients, [user])
    project = client.create_project(members=members)
    project = admin_user_client.wait_success(project)
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


def _create_project_with_members(admin_user_client, client, members):
    project = client.create_project(members=members)
    project = admin_user_client.wait_success(project)
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


def _create_members(user_clients, members):
    newMembers = []
    for member in members:
        newMembers.append({
            'role': 'owner' if member == 'Owner' else 'member',
            'externalId': acc_id(user_clients[member]),
            'externalIdType': 'rancher_id'
        })
    return newMembers


def all_owners(members):
    for member in members:
        member['role'] = 'owner'
    return members


@pytest.fixture(scope='session')
def user_clients(admin_user_client):
    clients = {}
    for user in _USER_LIST:
        clients[user] = create_context(admin_user_client, kind='user').user_client
    clients['admin'] = admin_user_client
    return clients


@pytest.fixture()
def members(user_clients):
    members = ['Owner', 'Member']
    return _create_members(user_clients, members)


def test_update_project(user_clients, project):
    user_clients['Owner'].update(
        project, name='Project Name', description='Some description')
    assert user_clients['Owner'].by_id(
        'project', project.id).name == 'Project Name'
    assert user_clients['Owner'].by_id(
        'project', project.id).description == 'Some description'
    with pytest.raises(ApiError) as e:
        user_clients['Member'].update(
            project, name='Project Name from Member', description='Loop hole?')
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        user_clients['Stranger'].update(
            project, name='Project Name from Stranger', description='Changed')
    assert e.value.error.status == 404


def test_set_members(admin_user_client, user_clients, project):
    members = get_plain_members(project.projectMembers())
    members.append({
        'role': 'member',
        'externalId': acc_id(user_clients['Member']),
        'externalIdType': 'rancher_id'
    })
    _set_members(admin_user_client, user_clients['Owner'], project.id, None,
                 422)
    _set_members(admin_user_client, user_clients['Owner'], project.id, [],
                 422)
    _set_members(admin_user_client, user_clients['Owner'], project.id,
                 members, None)
    _set_members(admin_user_client, user_clients['Member'], project.id,
                 None, 'Attribute')
    _set_members(admin_user_client, user_clients['Member'], project.id, [],
                 'Attribute')
    _set_members(admin_user_client, user_clients['Member'], project.id,
                 members, 'Attribute')
    with pytest.raises(ApiError) as e:
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, None, 422)
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, [], 422)
    assert e.value.error.status == 404
    with pytest.raises(ApiError) as e:
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, members, 403)
    assert e.value.error.status == 404


def test_get_members(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    members = project.projectMembers()
    _get_members(user_clients['Owner'], project.id, members)
    _get_members(user_clients['Member'], project.id, members)
    _get_members(user_clients['admin'], project.id, members)
    with pytest.raises(ApiError) as e:
        _get_members(user_clients['Stranger'], project.id, members)
    assert e.value.error.status == 404


def test_list_all_projects(admin_user_client):
    projects = admin_user_client.list_project()
    projectAccounts = admin_user_client.list_account(kind='project',
                                                     limit=4000)
    ids = []
    ids_2 = []
    for project in projects:
        ids.append(project.id)
    for project in projectAccounts:
        ids_2.append(project.id)
    assert len(list(set(ids) - set(ids_2))) == 0


def _create_resources(client):
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
        if type not in excludes:
            try:
                for resource in client.list(type, accountId=project_id):
                    assert resource.state in states
                    assert resource.removed is not None
            except AttributeError:
                pass


def client_for_project(project, admin_user_client):
    project_key = admin_user_client.create_api_key(accountId=project.id)
    admin_user_client.wait_success(project_key)
    return api_client(project_key.publicValue, project_key.secretValue)


def test_delete_project(admin_user_client, new_context,
                        super_client):
    project = new_context.user_client.reload(new_context.project)
    proj_id = new_context.project.id
    _create_resources(new_context.client)
    assert len(new_context.client.list_projectMember()) == 1
    project = super_client.wait_success(project.deactivate())
    project = super_client.wait_success(project.remove())
    check_state(new_context.client, proj_id,
                ['removed'], ['account', 'project', 'host', 'subscribe'])
    super_client.wait_success(project.purge())
    project = new_context.client.by_id('project', id=proj_id)
    assert project.state == 'purged'
    check_state(new_context.client, proj_id,
                ['purged', 'removed'], ['account', 'project', 'subscribe'])
    project_members = admin_user_client\
        .list('projectMember')
    for member in project_members:
        assert member.projectId != proj_id


def test_create_project_no_members(admin_user_client, user_clients):
    for client in user_clients.items():
        _create_project(admin_user_client, user_clients, client[0])


def test_delete_members(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    members = [members[0]]
    assert len(user_clients['Member']
               .by_id('project', project.id).projectMembers()) == 2
    project.setmembers(members=members)
    project = user_clients['Owner'].by_id('project', project.id)
    assert len(project.projectMembers()) == 1
    with pytest.raises(ApiError) as e:
        user_clients['Member'].by_id('project', project.id)
    assert e.value.error.status == 404


def test_change_roles(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    assert len(project.projectMembers()) == 2
    new_members = all_owners(get_plain_members(project.projectMembers()))
    project_from_member = user_clients['Member'].by_id('project',
                                                          project.id)
    with pytest.raises(AttributeError) as e:
        project_from_member.setmembers(members=new_members)
    assert 'setmembers' in e.value.message
    project.setmembers(members=new_members)
    project_from_member = user_clients['Member'].reload(project_from_member)
    project_from_member.setmembers(members=new_members)
    project_members_after = get_plain_members(project.projectMembers())
    project_from_member_members_after = get_plain_members(
        project_from_member.projectMembers())
    for member in project_members_after:
        assert member['role'] == 'owner'
    for member in project_from_member_members_after:
        assert member['role'] == 'owner'


def test_delete_other_owners(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'],
                                           all_owners(members))
    project.setmembers(members=members)
    project = user_clients['Member'].by_id('project', project.id)
    new_members = get_plain_members(project.projectMembers())
    for member in new_members:
        if((member['role'] == 'owner') & (member['externalId'] != acc_id(
                user_clients['Member']))):
            new_members.remove(member)
    project.setmembers(members=new_members)
    assert len(project.projectMembers()) == 1
    with pytest.raises(ApiError) as e:
        user_clients['Owner'].by_id('project', project.id)
    assert e.value.error.status == 404
    project = client_for_project(project, admin_user_client).list_project()[0]
    got_members = project.projectMembers()
    assert len(got_members) == 1


def test_multiple_owners_add_members(admin_user_client, user_clients,
                                     members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'],
                                           all_owners(members))
    current_members = get_plain_members(project.projectMembers())
    current_members.append({
        'role': 'member',
        'externalId': acc_id(user_clients['Stranger']),
        'externalIdType': 'rancher_id'
    })
    _set_members(admin_user_client, user_clients['Owner'], project.id,
                 current_members, None)
    _set_members(admin_user_client, user_clients['Stranger'], project.id,
                 current_members,
                 'Attribute')
    project = user_clients['Stranger'].by_id('project', project.id)
    assert len(project.projectMembers()) == 3
    _set_members(admin_user_client, user_clients['Member'], project.id,
                 members, None)
    with pytest.raises(ApiError) as e:
        project.projectMembers()
    assert e.value.error.status == 404
    _set_members(admin_user_client, user_clients['Member'], project.id,
                 current_members, None)
    assert len(project.projectMembers()) == len(current_members)


def test_members_cant_delete(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    project = user_clients['Member'].by_id("project", project.id)
    got_members = get_plain_members(project.projectMembers())
    id = acc_id(user_clients['Member'])
    for member in got_members:
        if member['externalId'] == id:
            assert member['role'] == 'member'
    with pytest.raises(ApiError) as e:
        user_clients['Member'].delete(project)
    assert e.value.error.status == 403
    _set_members(admin_user_client, user_clients['Member'], project.id, [{
        'externalId': acc_id(user_clients['Member']),
        'externalIdType': 'rancher_id',
        'role': 'owner'
    }], 'Attribute')


def test_project_cant_create_project(user_clients, members, project,
                                     admin_user_client):
    uuid = project.uuid
    client = client_for_project(project, admin_user_client)
    assert 'POST' not in client.schema.types['project'].collectionMethods
    got_project = client.list_project()[0]
    assert got_project.uuid == uuid
    assert len(project.projectMembers()) == len(got_project.projectMembers())
    pass


def test_accessible_projects(admin_user_client, user_clients):
    members = _create_members(user_clients, ['OutThereUser'])
    projects = []
    for i in range(0, 4):
        projects.append(_create_project_with_members(admin_user_client,
                                                     user_clients['admin'],
                                                     members))
    assert len(projects) == 4
    got_projects = user_clients['OutThereUser'].list_project()
    project_ids = get_ids(projects)
    got_project_ids = get_ids(got_projects)
    assert len(got_project_ids.intersection(project_ids)) == 4
    for project in projects:
        project.setmembers(members=_create_members(user_clients, ['Owner']))
    got_projects = user_clients['OutThereUser'].list_project()
    project_ids = get_ids(projects)
    got_project_ids = get_ids(got_projects)
    assert len(got_project_ids.intersection(project_ids)) == 0


def test_create_project_no_owner(user_clients):
    project = user_clients['admin'].create_project()
    project = user_clients['admin'].wait_success(project)
    PROJECTS.add(project.id)
    assert len(project.projectMembers()) == 1


def test_list_projects_flag(admin_user_client, user_clients):
    projects = admin_user_client.list('project')
    ids = set([])
    for project in projects:
        ids.add(project.id)
    projects_with_flag = admin_user_client.list('project', all='true')
    admin_id = acc_id(admin_user_client)
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


def test_get_project_not_mine(user_clients, project):
    with pytest.raises(ApiError) as e:
        user_clients['Member'].by_id('project', project.id)
    assert e.value.error.status == 404


def test_project_deactivate(user_clients, project, members):
    project.setmembers(members=members)
    diff_members(members, get_plain_members(project.projectMembers()))
    project = user_clients['Member'].reload(project)
    with pytest.raises(AttributeError) as e:
        project.deactivate()
    assert 'deactivate' in e.value.message
    project = user_clients['Owner'].reload(project)
    project.deactivate()
    project = user_clients['Owner'].wait_success(project)
    assert project.state == 'inactive'
    project.activate()
    project = user_clients['Owner'].wait_success(project)
    project.deactivate()
    project = user_clients['Owner'].wait_success(project)
    assert project.state == 'inactive'
