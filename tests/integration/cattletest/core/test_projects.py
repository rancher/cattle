from common_fixtures import *  # NOQA
from copy import deepcopy
from gdapi import ApiError


_USER_LIST = [
    "Owner",
    "Member",
    "Stranger",
    "OutThereUser"
]


PROJECTS = set([])


class NotFound(Exception):
    pass


@pytest.fixture(autouse=True, scope="module")
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


@pytest.fixture(scope='session')
def user_clients(admin_user_client):
    clients = {}
    for user in _USER_LIST:
        clients[user] = create_context(admin_user_client,
                                       kind='user').user_client
    clients['admin'] = admin_user_client
    return clients


@pytest.fixture()
def members(user_clients):
    members = ['Owner', 'Member']
    return _create_members(user_clients, members)


def get_ids(items):
    ids = []
    for item in items:
        ids.append(item.id)
    return set(ids)


def diff_members(members, got_members):
    assert len(members) == len(got_members)
    members_a = set([])
    members_b = set([])
    for member in members:
        members_a.add(
            member['externalId'] + '  ' + member['externalIdType'] + '  ' +
            member['role'])
    for member in got_members:
        members_b.add(
            member['externalId'] + '  ' + member['externalIdType'] + '  ' +
            member['role'])
    assert members_a == members_b


def all_owners(members):
    for member in members:
        member['role'] = 'owner'
    return members


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


def test_update_project_kubernetes_swarm(user_clients, project):
    project = user_clients['Owner'].update(
        project, name='Project Name', description='Some description',
        kubernetes=True, swarm=True)
    assert project.kubernetes is True
    assert project.swarm is True
    client = user_clients['Owner']
    members = _create_members(user_clients, ['Owner'])
    project = client.create_project(members=members,
                                    kubernetes=True, swarm=True)
    project = client.wait_success(project)
    assert project.kubernetes is True
    assert project.swarm is True


def test_project_no_filters_and_sort(user_clients):
    projects = user_clients['Owner'].list_project()
    assert len(projects.sortLinks) == 0
    assert len(projects.filters) == 0


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
    with pytest.raises(NotFound):
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, None, 422)
    with pytest.raises(NotFound):
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, [], 422)
    with pytest.raises(NotFound):
        _set_members(admin_user_client, user_clients['Stranger'],
                     project.id, members, 403)


def test_get_members(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    members = project.projectMembers()
    _get_members(user_clients['Owner'], project.id, members)
    _get_members(user_clients['Member'], project.id, members)
    _get_members(user_clients['admin'], project.id, members)
    with pytest.raises(NotFound):
        _get_members(user_clients['Stranger'], project.id, members)


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
    check_state(new_context.user_client, proj_id,
                ['removed'], ['account', 'project', 'host', 'subscribe'])
    super_client.wait_success(project.purge())
    project = new_context.user_client.by_id('project', id=proj_id)
    assert project.state == 'purged'
    check_state(new_context.user_client, proj_id,
                ['purged', 'removed'], ['account', 'project', 'subscribe'])
    project_members = admin_user_client\
        .list('projectMember')
    for member in project_members:
        assert member.projectId != proj_id


def test_delete_members(admin_user_client, user_clients, members):
    project = _create_project_with_members(admin_user_client,
                                           user_clients['Owner'], members)
    members = [members[0]]
    assert len(user_clients['Member']
               .by_id('project', project.id).projectMembers()) == 2
    project.setmembers(members=members)
    project = user_clients['Owner'].by_id('project', project.id)
    assert len(project.projectMembers()) == 1
    assert user_clients['Member'].by_id('project', project.id) is None


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
    assert user_clients['Owner'].by_id('project', project.id) is None
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


def test_members_cant_do_things(admin_user_client, user_clients, members):
    # Tests that members can't alter members of a project, delete a project,
    # deactivate a project.
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
    with pytest.raises(AttributeError) as e:
        project.deactivate()
    assert 'deactivate' in e.value.message


def test_project_cant_create_project(user_clients, members, project,
                                     admin_user_client):
    uuid = project.uuid
    client = client_for_project(project, admin_user_client)
    assert 'POST' not in client.schema.types['project'].collectionMethods
    got_project = client.list_project()[0]
    assert got_project.uuid == uuid
    assert len(project.projectMembers()) == len(got_project.projectMembers())
    pass


def test_list_projects_flag(admin_user_client, user_clients):
    projects = admin_user_client.list('project')
    ids = set([])
    for project in projects:
        ids.add(project.id)
    projects_with_flag = admin_user_client.list('project', all='true')
    admin_id = acc_id(admin_user_client)
    assert len(projects) != len(projects_with_flag)
    for project in projects_with_flag:
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
    assert user_clients['Member'].by_id('project', project.id) is None


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


def test_project_deactivate_members_cant_access(user_clients,
                                                project, members):
    project.setmembers(members=members)
    diff_members(members, get_plain_members(project.projectMembers()))
    project = user_clients['Owner'].reload(project)
    project.deactivate()
    project = user_clients['Owner'].wait_success(project)
    assert project.state == 'inactive'
    assert user_clients['Member'].by_id('project', project.id) is None
    project.activate()
    project = user_clients['Owner'].wait_success(project)
    prj_id = project.id
    assert project.state == 'active'
    project = user_clients['Member'].reload(project)
    assert project.id == prj_id


def test_project_member_invalid(project, admin_user_client, members):
    client = client_for_project(project, admin_user_client)
    project.setmembers(members=members)
    diff_members(members, get_plain_members(project.projectMembers()))
    members_got = get_plain_members(project.projectMembers())
    new_members = []
    old_members = []
    for member in members_got:
        if (member['role'] == 'owner'):
            new_members.append(member)
        else:
            old_members.append(member)
    project.setmembers(members=new_members)
    diff_members(new_members, get_plain_members(project.projectMembers()))
    assert client.by_id('projectMember', 'garbageId') is None
    assert client.by_id('projectMember', old_members[0]['externalId']) is None


def test_make_project_with_identity(admin_user_client):
    client = create_context(admin_user_client).user_client
    identity = client.list_identity()
    assert len(identity) == 1
    identity = identity[0]
    identity['role'] = 'owner'
    members = [identity]
    _create_project_with_members(admin_user_client, client, members)


def test_restricted_members(admin_user_client):
    context_1 = create_context(admin_user_client, create_project=True,
                               add_host=True)
    context_2 = create_context(admin_user_client)

    user2_client = context_2.user_client
    members = get_plain_members(context_1.project.projectMembers())
    members.append({
        'role': 'restricted',
        'externalId': acc_id(user2_client),
        'externalIdType': 'rancher_id'
    })
    project = context_1.user_client.reload(context_1.project)
    project.setmembers(members=members)

    user2_client = context_2.user_client

    new_headers = deepcopy(user2_client._headers)
    new_headers['X-API-Project-Id'] = project.id

    user2_client._headers = new_headers

    user2_client.reload_schema()

    hosts = user2_client.list_host()
    assert len(hosts) == 1
    assert hosts[0].actions == {}
    with pytest.raises(ApiError) as e:
        user2_client.delete(hosts[0])
    assert e.value.error.status == 405
    with pytest.raises(AttributeError) as e:
        client.list_registration_token()
    assert 'list_registration_token' in e.value.message
    with pytest.raises(AttributeError) as e:
        client.create_registration_token()
    assert 'create_registration_token' in e.value.message


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


def _set_members(admin_user_client, client, id, members, status):
    project = client.by_id('project', id)
    if project is None:
        raise NotFound()
    if status is None:
        project.setmembers(members=members)
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


def _get_members(client, id, actual_members):
    project = client.by_id('project', id)
    if project is None:
        raise NotFound()
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


def _create_members(user_clients, members):
    newMembers = []
    for member in members:
        newMembers.append({
            'role': 'owner' if member == 'Owner' else 'member',
            'externalId': acc_id(user_clients[member]),
            'externalIdType': 'rancher_id'
        })
    return newMembers
