from common_fixtures import *  # NOQA

compose = '''
test:
    image: nginx
'''


def test_project_orc(new_context):
    client = new_context.client
    owner_client = new_context.owner_client
    project = new_context.project

    assert project.orchestration == 'cattle'
    assert not project.virtualMachine

    owner_client.create_stack(name=random_str(),
                              dockerCompose=compose,
                              system=True,
                              externalId='catalog://infra:infra*k8s:1.4.0')
    owner_client.create_stack(name=random_str(),
                              dockerCompose=compose,
                              system=True,
                              externalId='catalog://infra:infra*'
                                         'virtualMachine:1.4.0')

    wait_for_condition(client, project, lambda x: x.orchestration ==
                       'kubernetes' and x.virtualMachine)
