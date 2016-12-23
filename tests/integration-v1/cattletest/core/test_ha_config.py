from common import *  # NOQA
import json


@pytest.mark.nonparallel
def test_ha_config(admin_user_client):
    ha_config = find_one(admin_user_client.list_ha_config)

    admin_user_client.update(ha_config, enabled=False)
    ha_config = find_one(admin_user_client.list_ha_config)
    assert not ha_config.enabled

    admin_user_client.update(ha_config, enabled=True)
    ha_config = find_one(admin_user_client.list_ha_config)
    assert ha_config.enabled

    admin_user_client.update(ha_config, enabled=False)
    ha_config = find_one(admin_user_client.list_ha_config)
    assert not ha_config.enabled

    assert ha_config.dbHost in ['localhost', '127.0.0.1']
    assert ha_config.dbSize > 0


def test_ha_config_script(admin_user_client):
    ha_config = find_one(admin_user_client.list_ha_config)
    create_url = ha_config.actions['createscript']
    r = requests.post(create_url, data=json.dumps({
        'clusterSize': 5,
        'httpPort': 1234,
        'httpsPort': 1235,
        'redisPort': 6375,
        'zookeeperQuorumPort': 6375,
        'zookeeperLeaderPort': 6375,
        'zookeeperClientPort': 6375,
        'cert': 'cert',
        'certChain': 'certChain',
        'key': 'key',
        'hostRegistrationUrl': 'https://....',
        'swarmEnabled': False,
        'httpEnabled': False,
    }))
    assert r.text is not None
    assert r.status_code == 200

    def check():
        ha_config = find_one(admin_user_client.list_ha_config)
        return ha_config.clusterSize == 5

    wait_for(check)


@pytest.mark.nonparallel
def test_ha_config_dbdump(admin_user_client):
    ha_config = find_one(admin_user_client.list_ha_config)
    dump = ha_config.links['dbdump']
    r = requests.get(dump)
    assert r.text is not None
    assert r.status_code == 200
