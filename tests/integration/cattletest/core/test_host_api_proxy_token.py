from common_fixtures import *  # NOQA
import base64
import json
from cattle import ApiError


@pytest.fixture(scope='module')
def host(super_client, context):
    return super_client.reload(context.host)


@pytest.fixture(scope='module')
def agent_cli(context):
    return context.agent_client


def test_host_api_proxy_token_create(host, agent_cli):
    # Token should be created with the supplied reportedUuid in it.
    token = agent_cli.create_host_api_proxy_token(
        reportedUuid=host.data.fields['reportedUuid'])
    assert token is not None

    parts = token.token.split('.')
    encoded_claims = parts[1]
    encoded_claims += '=' * (4 - (len(encoded_claims) % 4))
    claims = base64.decodestring(encoded_claims)
    claims_obj = json.loads(claims)
    assert claims_obj['reportedUuid'] == host.data.fields['reportedUuid']


def test_bad_host(host, new_context):
    # If a host doesn't belong to agent submitting the request, it should fail.
    agent_cli = new_context.agent_client

    with pytest.raises(ApiError) as e:
        agent_cli.create_host_api_proxy_token(
            reportedUuid=host.data.fields['reportedUuid'])

    assert e.value.error.code == 'InvalidReference'
