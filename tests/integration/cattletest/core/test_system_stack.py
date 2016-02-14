from common_fixtures import *  # NOQA


def test_project_update(new_context):
    client = new_context.client
    user_client = new_context.user_client
    assert new_context.project.swarm is False

    stacks = client.list_environment()
    assert len(stacks) == 0

    p = user_client.update(new_context.project, swarm=True)
    assert p.state == 'updating-active'

    p = user_client.wait_success(p)
    assert p.state == 'active'
    assert p.swarm

    def get_stack():
        stacks = client.list_environment()
        if len(stacks) > 0:
            return stacks[0]

    stack = wait_for(get_stack)
    stack = client.wait_success(stack)
    assert stack.name == 'Swarm'
    assert stack.externalId == 'system://swarm'
    assert stack.dockerCompose is not None
    assert stack.startOnCreate

    p = user_client.update(new_context.project, swarm=False)
    assert p.state == 'updating-active'
    p = user_client.wait_success(p)
    assert p.state == 'active'
    assert not p.swarm

    def get_no_stack():
        stacks = client.list_environment(removed_null=True)
        if len(stacks) > 0:
            assert stacks[0].externalId == 'system://swarm'
        else:
            return True

    wait_for(get_no_stack)
