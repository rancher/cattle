from common_fixtures import *  # NOQA


def test_project_template(new_context):
    user_client = new_context.user_client
    entry = {
        'name': 'foo-' + random_str(),
        'dockerCompose': 'test:\n  image: nginx'
    }
    entry2 = {
        'name': 'foo-' + random_str(),
        'templateVersionId': 'foo:infra*k8s'
    }
    template = user_client.create_project_template(stacks=[entry, entry2])
    template = user_client.wait_success(template)
    assert template.state == 'active'
    assert len(template.stacks) == 2
    assert template.stacks[0].name == entry['name']
    assert template.stacks[0].dockerCompose == entry['dockerCompose']
    assert template.stacks[1].name == entry2['name']
    assert template.stacks[1].templateVersionId == 'foo:infra*k8s'

    return template


def test_project_from_template(new_context, super_client):
    template = test_project_template(new_context)
    user_client = new_context.user_client
    proj = user_client.create_project(projectTemplateId=template.id)
    assert proj.orchestration == 'kubernetes'

    proj = user_client.wait_success(proj)
    assert proj.state == 'active'

    proj = super_client.reload(proj)
    proj = wait_for_condition(super_client, proj,
                              lambda x: x.createdStackIds is not None)
    assert len(proj.createdStackIds) == 1 or len(proj.createdStackIds) == 2
