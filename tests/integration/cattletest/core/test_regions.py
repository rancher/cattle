from common_fixtures import *  # NOQA


def test_region_create(admin_user_client):
    name = random_str()
    p_v = 'foo'
    s_v = 'var'
    url = "http://foo"
    region = admin_user_client.create_region(name=name,
                                             publicValue=p_v,
                                             secretValue=s_v,
                                             url=url)
    assert region.name == name
    assert region.url == url
    assert region.publicValue == p_v
    assert region.secretValue == s_v
    assert region.local is False


def test_region_create_self(admin_user_client):
    name = random_str()
    p_v = 'foo'
    s_v = 'var'
    url = "http://foo"
    region = admin_user_client.create_region(name=name,
                                             publicValue=p_v,
                                             secretValue=s_v,
                                             url=url,
                                             local=True)
    assert region.local is True


def test_region_create_service_account_and_agent(admin_user_client):
    name = random_str()

    sa = admin_user_client.create_account(name=name,
                                          kind="service")
    assert sa.name == name
    assert sa.kind == "service"

    public_value = random_str()
    secret_value = random_str()
    sa_key = admin_user_client.create_api_key(accountId=sa.id,
                                              publicValue=public_value,
                                              secretValue=secret_value)
    assert sa_key.state == 'registering'
    assert sa_key.publicValue == public_value
    assert sa_key.secretValue == secret_value
    assert sa_key.accountId == sa.id

    cs = admin_user_client.list_credential(accountId=sa.id)
    assert len(cs) == 1

    service_client = api_client(sa_key.publicValue, sa_key.secretValue)

    # create agent
    public_value = random_str()
    secret_value = random_str()
    name = "region_environment_agent"
    uri = "event:///external=" + random_str()
    agent = service_client.create_agent(name=name, uri=uri,
                                        publicValue=public_value,
                                        secretValue=secret_value)
    assert agent.name == name
    assert agent.uri == uri
