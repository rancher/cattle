import testcommon
import sys
print sys.path
from dstack.docker.storage import Template
from dstack.docker.storage import DockerPool
from dstack.marshaller import JsonObject
import pytest


@pytest.fixture(scope="module")
def dockerpool():
    return DockerPool()

def test_stage_template(dockerpool):
    dockerpool.delete_template(templateStoragePoolRef = JsonObject({
        "data" : {
            "docker.image.Repository" : "ibuildthecloud/hello-world"
        }
    }))
    template = dockerpool.stage_template(template = JsonObject({
        "data" : {
            "docker.image.Repository" : "ibuildthecloud/hello-world"
        }
    }))
    assert not template is None
    assert template["Repository"] == "ibuildthecloud/hello-world"
    assert template["Tag"] == "latest"
