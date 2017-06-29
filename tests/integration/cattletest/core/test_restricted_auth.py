from common_fixtures import *  # NOQA
import copy

from cattle import ApiError
from test_authorization import service_client  # NOQA
from test_machines import machine_context
from test_svc_discovery import _validate_compose_instance_start

_ = machine_context  # Needed just to avoid a pep-8 quirk


@pytest.fixture()
def infra_access_setting(admin_user_client):
    id = 'modify.infrastructure.roles'
    setting = admin_user_client.by_id_setting(id)
    orig = setting.value
    no_member = orig.replace('member,', '')
    setting = admin_user_client.update(setting, value=no_member)
    wait_setting_active(admin_user_client, setting)
    yield
    setting = admin_user_client.by_id_setting(id)
    setting = admin_user_client.update(setting, value=orig)
    wait_setting_active(admin_user_client, setting)


@pytest.mark.nonparallel
def test_restricted_infra_access(new_context, admin_user_client,
                                 infra_access_setting, machine_context):
    client = new_context.client
    member = create_user(new_context, admin_user_client, 'member')
    host = new_context.host

    # No actions on host
    m_host = member.by_id_host(host.id)
    assert len(m_host.actions) == 0
    # Can't create host
    with pytest.raises(ApiError) as e:
        member.create_host(hostname='foo')
    assert e.value.error.status == 403
    # Can't update host
    with pytest.raises(ApiError) as e:
        member.update(m_host, name='foo')
    assert e.value.error.status == 403
    # Can't delete host
    with pytest.raises(ApiError) as e:
        member.delete(m_host)
    assert e.value.error.status == 403

    reg_tokens = client.list_registration_token()
    assert len(reg_tokens) > 0
    reg_token = reg_tokens[0]

    # Can't see any registration tokens
    m_reg_tokens = member.list_registration_token()
    assert len(m_reg_tokens) == 0
    # Can't create registration token
    with pytest.raises(ApiError) as e:
        member.create_registration_token()
    assert e.value.error.status == 403
    # Can't update registraion token
    with pytest.raises(ApiError) as e:
        member.update(reg_token, name=random_str())
    assert e.value.error.status == 405
    # Can't delete registration token
    with pytest.raises(ApiError) as e:
        member.delete(reg_token)
    assert e.value.error.status == 405

    # Physical host has no actions
    ph = m_host.physicalHost()
    assert len(ph.actions) == 0
    # Can't update physical host
    with pytest.raises(ApiError) as e:
        member.update(ph, name=random_str())
    assert e.value.error.status == 405
    # Can't delete physical host
    with pytest.raises(ApiError) as e:
        member.delete(ph)
    assert e.value.error.status == 405

    # Owner creates machine
    machine = client.create_machine(name=random_str(), fooConfig={})
    machine = client.wait_success(machine)
    # Machine has no actions
    mem_machine = member.by_id_machine(machine.id)
    assert len(mem_machine.actions) == 0
    # Can't create machine
    with pytest.raises(ApiError) as e:
        member.create_machine(name=random_str(), fooConfig={})
    assert e.value.error.status == 403
    # Can't update machine
    with pytest.raises(ApiError) as e:
        member.update(mem_machine, name=random_str())
    assert e.value.error.status == 403
    # Can't delete machine
    with pytest.raises(ApiError) as e:
        member.delete(mem_machine)
    assert e.value.error.status == 403


def test_restricted_from_system(new_context, admin_user_client):
    restricted = create_user(new_context, admin_user_client)

    # Restricted can't create system stack. system property not settable
    rstack = restricted.create_stack(name=random_str(), system=True)
    rstack = restricted.wait_success(rstack)
    assert rstack.system is False

    # Owner can create system stack
    owner = new_context.owner_client
    stack = owner.wait_success(owner.create_stack(name="test", system=True))
    assert stack.state == "active"
    assert stack.system is True

    # Restricted cant update system stack
    rstack = restricted.by_id_stack(stack.id)
    assert rstack.name == "test"
    with pytest.raises(ApiError) as e:
        restricted.update(rstack, name="updated")
    assert e.value.error.status == 403

    # Restricted user should see no actions on system stack
    assert len(rstack.actions) == 0
    assert len(stack.actions) > 0

    # Owner can update stack
    stack = owner.update(stack, name="updated")

    # Restricted can't create service in system stack
    lc = {"imageUuid": new_context.image_uuid}
    with pytest.raises(ApiError) as e:
        restricted.create_service(name=random_str(), stackId=stack.id,
                                  launchConfig=lc)
    assert e.value.error.status == 403

    # Owner can create service in system stack
    svc = owner.create_service(name=random_str(), stackId=stack.id,
                               launchConfig=lc, scale=1)
    svc = owner.wait_success(svc)

    # Owner can activate
    svc = owner.wait_success(svc.activate())

    c = _validate_compose_instance_start(owner, svc, stack, "1")

    # Owner can update system service
    svc = owner.update(svc, name="update")
    svc = owner.wait_success(svc)

    # Restricted can't update system service
    rsvc = restricted.by_id_service(svc.id)
    with pytest.raises(ApiError) as e:
        restricted.update(rsvc, name="update")
    assert e.value.error.status == 403

    # Restricted user should see no actions on system service
    assert len(rsvc.actions) == 0
    assert len(svc.actions) > 0

    # Restricted can't delete system service
    with pytest.raises(ApiError) as e:
        restricted.delete(rsvc)
    assert e.value.error.status == 403

    # Restricted can't update system container
    rc = restricted.by_id_container(c.id)
    with pytest.raises(ApiError) as e:
        restricted.update(rc, name="update")
    assert e.value.error.status == 403

    # Owner can update system container
    c = owner.update(c, name="update")

    # Restricted can only see the logs actions of system containers
    assert len(rc.actions) == 1 and rc.actions["logs"]
    assert len(c.actions) > 1

    # Restricted can't delete system container
    rc = restricted.by_id_container(c.id)
    with pytest.raises(ApiError) as e:
        restricted.delete(rc)
    assert e.value.error.status == 403

    # Owner can delete system container
    owner.delete(c)

    # Owner can delete system service
    owner.delete(svc)

    # Restricted can't delete system stack
    with pytest.raises(ApiError) as e:
        restricted.delete(rstack)
    assert e.value.error.status == 403

    # Owner can delete system stack
    owner.delete(stack)

    # Restricted user can do all the above things for non-system resources
    stack = restricted.wait_success(restricted.create_stack(name="restricted"))
    assert stack.state == "active"
    assert stack.system is False
    stack = restricted.update(stack, name="r-updated")
    assert len(stack.actions) > 0
    svc = restricted.create_service(name=random_str(), stackId=stack.id,
                                    launchConfig=lc)
    svc = restricted.wait_success(svc)
    svc = restricted.wait_success(svc.activate())
    assert len(svc.actions) > 0
    c = _validate_compose_instance_start(restricted, svc, stack, "1")
    c = restricted.update(c, name="r-updated")
    assert len(c.actions) > 1
    svc = restricted.update(svc, name="r-updated")
    restricted.delete(c)
    restricted.delete(svc)
    restricted.delete(stack)


def test_restricted_agent_containers(new_context, admin_user_client):
    restricted = create_user(new_context, admin_user_client)
    client = new_context.client
    c = new_context.create_container(labels={
        'io.rancher.container.create_agent': 'true'
    })
    c = client.wait_success(c)
    assert c.actions["execute"]
    assert c.actions["proxy"]

    rc = restricted.by_id_container(c.id)
    assert "execute" not in rc.actions
    assert "proxy" not in rc.actions


def test_restricted_privileged_cap_add(new_context, admin_user_client):
    restricted = create_user(new_context, admin_user_client)
    client = new_context.client
    c = new_context.create_container(privileged=True)
    c = client.wait_success(c)
    assert c.actions["execute"]
    assert c.actions["proxy"]
    rc = restricted.by_id_container(c.id)
    assert "execute" not in rc.actions
    assert "proxy" not in rc.actions

    c = new_context.create_container(capAdd=["ALL"])
    c = client.wait_success(c)
    assert c.actions["execute"]
    assert c.actions["proxy"]
    rc = restricted.by_id_container(c.id)
    assert "execute" not in rc.actions
    assert "proxy" not in rc.actions


def create_user(context, admin_user_client, role='restricted'):
    context2 = create_context(admin_user_client)
    restricted = context2.user_client
    members = get_plain_members(context.project.projectMembers())
    members.append({
        'role': role,
        'externalId': acc_id(restricted),
        'externalIdType': 'rancher_id'
    })
    project = context.user_client.reload(context.project)
    project.setmembers(members=members)

    restricted = context2.user_client
    new_headers = copy.deepcopy(restricted._headers)
    new_headers['X-API-Project-Id'] = project.id
    restricted._headers = new_headers
    restricted.reload_schema()
    return restricted
