import pytest
from os.path import dirname
from datadiff.tools import assert_equals
import os
import sys

_file = os.path.abspath(__file__)
sys.path.insert(0, dirname(dirname(_file)))

import tests
import shutil
from .response_holder import ResponseHolder
from cattle import type_manager, plugins
from cattle.agent import Agent
from cattle.utils import JsonObject


plugins.load()


TEST_DIR = os.path.join(dirname(tests.__file__))
SCRATCH_DIR = os.path.join(TEST_DIR, 'scratch')

if os.path.exists(SCRATCH_DIR):
    shutil.rmtree(SCRATCH_DIR)
os.makedirs(SCRATCH_DIR)


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


def _diff_dict(left, right):
    for k in left.keys():
        left_value = left.get(k)
        right_value = right.get(k)
        try:
            _diff_dict(dict(left_value), dict(right_value))
            assert_equals(dict(left_value), dict(right_value))
        except AssertionError, e:
            raise e
        except:
            pass


def event_test(agent, name, pre_func=None, post_func=None):
    req = json_data(name)
    resp_valid = json_data(name + '_resp')

    if pre_func is not None:
        pre_func(req)

    resp = agent.execute(req)
    if post_func is not None:
        post_func(req, resp)

    del resp["id"]
    del resp["time"]

    _diff_dict(JsonObject.unwrap(resp_valid), JsonObject.unwrap(resp))
    assert_equals(JsonObject.unwrap(resp_valid), JsonObject.unwrap(resp))

    return req, resp
