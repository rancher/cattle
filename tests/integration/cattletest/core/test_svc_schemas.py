from common_fixtures import *  # NOQA


def _create_stack(client):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_service_schema(client, context, admin_user_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    k8s = {"resourceFields": {"template": {"type": "json",
                                           "create": True, "read": True}},
           "collectionMethods": ["GET", "POST", "DELETE"],
           "resourceMethods": ["GET", "POST"]}
    launch_config = {
        "imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    serviceSchemas={'kubernetesService': k8s})

    client.wait_success(service)

    assert service.serviceSchemas is not None

    client.reload_schema()

    # Test creation
    k8s_svc = client.create_kubernetesService(name=random_str(),
                                              environmentId=env.id,
                                              template={"foo": "bar"})

    k8s_svc = client.wait_success(k8s_svc)
    assert k8s_svc.template is not None
    assert k8s_svc.kind == "kubernetesService"

    # Test removal
    k8s_svc = client.wait_success(k8s_svc.remove())
    assert k8s_svc.state == 'removed'
