from common_fixtures import *
import pytest
from dstack import config

if_docker = pytest.mark.skipif('os.environ.get("DOCKER_TEST") != "true"', reason="DOCKER_TEST is not set")


@if_docker
def test_image_activate(agent, responses):
    event_test(agent, 'docker/image_activate')


@if_docker
def test_volume_activate(agent, responses):
    event_test(agent, 'docker/volume_activate')


@if_docker
def test_volume_deactivate(agent, responses):
    event_test(agent, 'docker/volume_deactivate')


@if_docker
def test_instance_activate(agent, responses):
    def post(req, resp):
        del resp["data"]["+data"]["dockerContainer"]["Created"]
        del resp["data"]["+data"]["dockerContainer"]["Id"]
        del resp["data"]["+data"]["dockerContainer"]["Status"]

    event_test(agent, 'docker/instance_activate', post_func=post)


@if_docker
def test_instance_deactivate(agent, responses):
    config.STOP_TIMEOUT_SECONDS = 1

    test_instance_activate(agent, responses)

    def post(req, resp):
        del resp["data"]["+data"]["dockerContainer"]["Created"]
        del resp["data"]["+data"]["dockerContainer"]["Id"]
        del resp["data"]["+data"]["dockerContainer"]["Status"]

    event_test(agent, 'docker/instance_deactivate', post_func=post)
