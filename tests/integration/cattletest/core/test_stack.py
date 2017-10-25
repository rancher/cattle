from common_fixtures import *  # NOQA
from cattle import ApiError

binding_input = {
    "services":
        {
            "service_1":
            {
                "labels": {"label_1": "value_1"},
                "ports": []
            }
        }
    }


def _create_stack(client):
    env = client.create_stack(name=random_str(), binding=binding_input)
    env = client.wait_success(env)
    return env


def test_create_stack_binding(client):
    env = _create_stack(client)
    del env.binding.__dict__['type']
    assert env.binding is not None
    assert "service_1" in env.binding.services
    assert env.binding.services.service_1.labels == {"label_1": "value_1"}
    assert env.binding.services.service_1.ports == []


def test_validate_stack_name(client):
    # stack_name starting/ending in hyphen
    for stack_name in ("-"+random_str(), random_str()+"-"):
        with pytest.raises(ApiError) as e:
            client.create_stack(name=stack_name)
        assert e.value.error.status == 422
        e.value.error.code == 'InvalidCharacters'


def test_duplicate_stack_name(client):
    env = _create_stack(client)
    env2 = _create_stack(client)
    with pytest.raises(ApiError) as e:
        client.create_stack(name=env.name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'
    with pytest.raises(ApiError) as e:
        client.update(env, name=env2.name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'
