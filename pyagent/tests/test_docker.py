import testcommon
import sys
print sys.path
from dstack.docker.storage import Template
from dstack.docker.storage import DockerPool
from dstack.docker.compute import DockerCompute
from dstack.marshaller import JsonObject
import pytest


@pytest.fixture(scope="module")
def dockerpool():
    return DockerPool()

@pytest.fixture(scope="module")
def dockercompute():
    return DockerCompute()

def test_stage_template(dockerpool, name="ibuildthecloud/hello-world", delete=True):
    if delete:
        dockerpool.delete_template(templateStoragePoolRef = JsonObject({
            "data" : {
                "docker.image.Repository" : name
            }
        }))
    template = dockerpool.stage_template(template = JsonObject({
        "data" : {
            "docker.image.Repository" : name
        }
    }))
    assert not template is None
    assert template["Repository"] == name
    assert template["Tag"] == "latest"

    return template

def test_create_container(dockercompute, dockerpool):
    #test_stage_template(dockerpool, "busybox", delete=False)
    vm = JsonObject({
        "data" : {
            "docker.container.Image" : "busybox",
            "docker.container.Cmd" : [ "/bin/sleep",  "15" ],
            "docker.container.Name" : ""
        }
    })

    import pdb; pdb.set_trace()
    container = dockercompute.start(vm)
    assert not container is None
    assert not container.get("Id") is None
    assert container.get("Image") == "busybox:latest"
    assert container.get("Command") == "/bin/sleep 15"
