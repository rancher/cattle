from common_fixtures import *  # NOQA

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
