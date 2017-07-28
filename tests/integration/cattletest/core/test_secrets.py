from common_fixtures import *  # NOQA
import yaml
from cattle import ApiError
import requests


key = "-----BEGIN RSA PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMII" \
      "BCgKCAQEA2M3kVGHBC4XQIWuvqXvg\n+QYc0ELwcWjDrZrFZwVHwuqlXVUs2pZ0caJP" \
      "vXUfinMw2z0St4Fa6bqjBGwvZu3o\nEPeh4P1f7M2bab0vHlwKaFPD7uYuGbLmwOhYO" \
      "1YdO5LNwU0jmhOBfF+Jr1cET7gQ\nDQ+/btyO3gnkd85LQ9QEnoVWrDgNyBNYwUl5uu" \
      "4RtwwgJQ7meuDWvFllQhZ8awdL\nt8akxmQRXHpVZmR3lmj+yAqc9nXXjg3yahzxyGt" \
      "oicbV/CxbMOpeThMakh1Y+9Bv\nWmJS3UAMLorvdZBmvqaSeRGGHhCYgZ4nO+evNJAt" \
      "g/0sSoeSM3h3uPHvWaq2xnKX\nTQIDAQAB\n-----END RSA PUBLIC KEY-----\n"


@pytest.fixture
def secret_context(new_context, super_client):
    super_client.update(new_context.host, info={
        'hostKey': {
            'data': key
        }
    })

    client = new_context.client
    s = client.create_stack(name=random_str())
    s = super_client.update(s, system=True)
    service = client.create_storage_driver_service(
        name=random_str(),
        startOnCreate=True,
        stackId=s.id,
        storageDriver={
            'volumeCapabilities': ['secrets'],
            'scope': 'local',
        })
    service = client.wait_success(service)
    assert service.state == 'active'
    return new_context


def test_secret_create_bad(secret_context):
    client = secret_context.client
    with pytest.raises(ApiError):
        client.create_secret(name=random_str(), value='!!!')


def test_secret_create_and_delete(secret_context):
    client = secret_context.client
    secret = client.create_secret(name=random_str(),
                                  value='foo')
    assert secret.state == 'creating'
    assert secret.value == 'foo'
    assert secret.description is None
    secret = client.wait_success(secret)

    secret = client.update(secret, description='foo')
    assert secret.description == 'foo'

    client.delete(secret)


def test_secret_create_and_use(secret_context):
    client = secret_context.client
    secret = client.create_secret(name=random_str(),
                                  value='foo')
    assert secret.state == 'creating'
    assert secret.value == 'foo'
    secret = client.wait_success(secret)

    secret = client.reload(secret)
    assert secret.value is None

    c = secret_context.create_container(
        secrets=[
            {
                'name': 'blah',
                'uid': 'user',
                'gid': 'group',
                'mode': '0400',
                'secretId': secret.id,
            }
        ]
    )
    c = client.wait_success(c)
    assert c.state == 'running'

    assert len(c.secrets) == 1
    assert c.secrets[0].name == 'blah'
    assert c.secrets[0].uid == 'user'
    assert c.secrets[0].gid == 'group'
    assert c.secrets[0].mode == '0400'
    assert c.secrets[0].secretId == secret.id

    volumes = c.dataVolumeMounts
    assert len(volumes) == 1
    assert '/run/secrets' in volumes

    volume = client.by_id_volume(volumes['/run/secrets'])
    assert 'secrets' in volume.storageDriver().volumeCapabilities
    assert len(volume.driverOpts['io.rancher.secrets.token']) > 0
    assert len(volume.name) == 64


def test_secret_create_and_download(secret_context, super_client):
    client = secret_context.client
    secret = client.create_secret(name=random_str(),
                                  value='foo')
    secret = client.wait_success(secret)

    c = secret_context.create_container(
        labels={
            'io.rancher.container.create_agent': True,
            'io.rancher.container.agent.role': 'agent',
        },
        secrets=[
            {
                'name': 'blah',
                'uid': 'user',
                'gid': 'group',
                'mode': '0400',
                'secretId': secret.id,
            }
        ]
    )
    c = client.wait_success(c)
    assert c.state == 'running'

    volume = client.by_id_volume(c.dataVolumeMounts['/run/secrets'])
    volume = client.wait_success(volume)
    token = volume.driverOpts['io.rancher.secrets.token']
    assert len(token) > 0

    ci = super_client.reload(c)
    cred = ci.agent().account().credentials()[0]

    base = client.schema.types['secret'].links.collection
    r = requests.request('POST', base + '/secrets',
                         auth=(cred.publicValue, cred.secretValue),
                         headers={
                             'Content-Type': 'application/x-api-secrets-token',
                         },
                         data=token)
    assert r.status_code == 200
    resp = r.json()

    assert resp[0]['name'] == 'blah'
    assert resp[0]['uid'] == 'user'
    assert resp[0]['gid'] == 'group'
    assert resp[0]['mode'] == '0400'
    assert len(resp[0]['rewrapText']) == 908


def test_secret_multi_host(secret_context, super_client):
    host2 = register_simulated_host(secret_context)

    client = secret_context.client
    secret = client.create_secret(name=random_str(),
                                  value='foo')
    secret = client.wait_success(secret)

    secrets = [{'name': 'blah', 'secretId': secret.id}]
    c = secret_context.create_container(secrets=secrets,
                                        requestedHostId=secret_context.host.id)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.hosts()[0].id == secret_context.host.id

    c = secret_context.create_container(secrets=secrets,
                                        requestedHostId=host2.id)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.hosts()[0].id == host2.id


def test_service_secrets_fields(context, secret_context):
    client = secret_context.client
    env = _create_stack(client)

    image_uuid = secret_context.image_uuid

    sclient = secret_context.client
    secret1 = sclient.create_secret(name=random_str(),
                                    value='foo')
    assert secret1.state == 'creating'
    assert secret1.value == 'foo'
    secret1 = sclient.wait_success(secret1)

    secret1 = sclient.reload(secret1)
    assert secret1.value is None

    secret2 = sclient.create_secret(name=random_str(),
                                    value='bar')
    assert secret2.state == 'creating'
    assert secret2.value == 'bar'
    secret2 = sclient.wait_success(secret2)

    secret2 = sclient.reload(secret2)
    assert secret2.value is None

    secrets = [
        {
           "name": "my_secret1",
           "secretId": secret1.id,
        },
        {
            "name": "my_secret2",
            "secretId": secret2.id,
            "mode": "444",
            "uid": "0",
            "gid": "0",
        }
    ]
    launch_config = {"imageUuid": image_uuid,
                     "secrets": secrets,
                     }
    service = client. \
        create_service(name=random_num(),
                       stackId=env.id,
                       launchConfig=launch_config,
                       retainIp=True
                       )
    service = client.wait_success(service)

    compose_config = env.exportconfig()
    docker_yml = yaml.load(compose_config.dockerComposeConfig)
    assert "version" in docker_yml
    assert docker_yml["version"] == "2"
    svc = docker_yml['services'][service.name]
    assert len(docker_yml["secrets"]) == 2
    assert secret1.name in docker_yml["secrets"].keys()
    assert secret2.name in docker_yml["secrets"].keys()
    assert len(svc['secrets']) == 2
    assert svc['secrets'][0] == secret1.name
    assert svc['secrets'][1]['uid'] == '0'
    assert svc['secrets'][1]['gid'] == '0'
    assert svc['secrets'][1]['mode'] == 444
    assert svc['secrets'][1]['source'] == secret2.name
    assert svc['secrets'][1]['target'] == 'my_secret2'


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env
