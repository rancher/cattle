import pytest
import os.path
from os.path import dirname
import os
import sys

_file = os.path.abspath(__file__)
sys.path.insert(0, dirname(dirname(_file)))

import tests
from .response_holder import ResponseHolder
from dstack import type_manager, plugins
from dstack.agent import Agent


plugins.load()


TEST_DIR = os.path.join(dirname(tests.__file__))


@pytest.fixture(scope="module")
def responses():
    r = ResponseHolder()
    type_manager.register_type(type_manager.PUBLISHER, r)
    return r


@pytest.fixture(scope="module")
def agent(responses):
    return Agent()


def json_data(name):
    marshaller = type_manager.get_type(type_manager.MARSHALLER)
    with open(os.path.join(TEST_DIR, name)) as f:
        return marshaller.from_string(f.read())


def event_test(agent, name, post_func=None):
    req = json_data(name)
    resp_valid = json_data(name + '_resp')
    resp = agent.execute(req)
    if post_func is not None:
        post_func(req, resp)

    del resp["id"]
    del resp["time"]

    assert dict(resp) == dict(resp_valid)

    return req, resp
