from common_fixtures import *  # NOQA
import copy

from cattle import ApiError
from test_svc_discovery import _validate_compose_instance_start


def test_restricted_from_system(new_context, admin_user_client):
    restricted = create_restricted_user(new_context, admin_user_client)

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
                               launchConfig=lc)
    svc = owner.wait_success(svc)
    svc = owner.wait_success(svc.activate())

    # Restricted can't update system service
    rsvc = restricted.by_id_service(svc.id)
    with pytest.raises(ApiError) as e:
        restricted.update(rsvc, name="update")
    assert e.value.error.status == 403

    # Owner can update system service
    svc = owner.update(svc, name="update")

    # Restricted user should see no actions on system service
    assert len(rsvc.actions) == 0
    assert len(svc.actions) > 0

    # Restricted can't delete system service
    with pytest.raises(ApiError) as e:
        restricted.delete(rsvc)
    assert e.value.error.status == 403

    c = _validate_compose_instance_start(owner, svc, stack, "1")

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
    svc = restricted.update(svc, name="r-updated")
    c = _validate_compose_instance_start(restricted, svc, stack, "1")
    c = restricted.update(c, name="r-updated")
    assert len(c.actions) > 1
    restricted.delete(c)
    restricted.delete(svc)
    restricted.delete(stack)


def test_restricted_agent_containers(new_context, admin_user_client):
    restricted = create_restricted_user(new_context, admin_user_client)
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
    restricted = create_restricted_user(new_context, admin_user_client)
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


def create_restricted_user(context, admin_user_client):
    context2 = create_context(admin_user_client)
    restricted = context2.user_client
    members = get_plain_members(context.project.projectMembers())
    members.append({
        'role': 'restricted',
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
