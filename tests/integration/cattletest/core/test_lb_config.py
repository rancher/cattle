from common_fixtures import *  # NOQA
from cattle import ApiError


def test_lb_config_create_wo_listener(client):
    config = client.create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    assert config.state == 'active'


def test_lb_config_add_listener(client):
    config, listener = _create_config_and_listener(client)

    # add listener to the config
    config = config.addlistener(loadBalancerListenerId=listener.id)

    validate_add_listener(config, listener, client)


def test_create_config_add_invalid_listener(client):
    config, listener = _create_config_and_listener(client)

    with pytest.raises(ApiError) as e:
        config.addlistener(loadBalancerListenerId='dummy')

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'loadBalancerListenerId'


def test_lb_config_add_listener_wo_listenerId(client):
    config, listener = _create_config_and_listener(client)

    # add listener to the config without specifying the id
    with pytest.raises(ApiError) as e:
        config.addlistener()

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerListenerId'


def test_lb_config_remove_listener(client):
    config, listener = _create_config_and_listener(client)

    # add listener to the config
    config = config.addlistener(loadBalancerListenerId=listener.id)

    assert config.state == 'active'
    validate_add_listener(config, listener, client)

    # remove listener from the config
    config = config. \
        removelistener(loadBalancerListenerId=listener.id)

    validate_remove_listener(config, listener, client)


def test_lb_config_delete_listener_wo_listenerId(client):
    config, listener = _create_config_and_listener(client)

    # remove listener to the config without specifying the id
    with pytest.raises(ApiError) as e:
        config.removelistener()

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerListenerId'


def test_lb_config_update(client):
    config = client.create_loadBalancerConfig(name='config1')
    config = client.wait_success(config)

    config = client.update(config, name='newName')
    assert config.name == 'newName'


def test_lb_config_remove(client):
    config = client.create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    config = client.wait_success(client.delete(config))

    assert config.state == 'removed'


def validate_add_listener(config, listener, client):
    lb_config_maps = client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=listener.id,
                                           loadBalancerConfigId=config.id)
    assert len(lb_config_maps) == 1
    config_map = lb_config_maps[0]
    wait_for_condition(
        client, config_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def validate_remove_listener(config, listener, client):
    lb_config_maps = client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=listener.id,
                                           loadBalancerConfigId=config.id)
    assert len(lb_config_maps) == 1
    config_map = lb_config_maps[0]
    wait_for_condition(
        client, config_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_lb_config_listener_remove(client):
    # create config and listener
    config, listener = _create_config_and_listener(client)

    # add a listener to the config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config, listener, client)

    # delete the listener
    listener = client.wait_success(client.delete(listener))
    assert listener.state == 'removed'

    # verify that the mapping is gone
    validate_remove_listener(config, listener, client)


def create_two_configs_w_listener(client, listener):
    config1 = client. \
        create_loadBalancerConfig(name=random_str())
    config1 = client.wait_success(config1)
    config1 = config1. \
        addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config1, listener, client)

    config2 = client. \
        create_loadBalancerConfig(name=random_str())
    config2 = client.wait_success(config2)
    config2 = config2. \
        addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config2, listener, client)

    return config1, config2


def test_lb_listener_remove(client):
    # create a listener
    listener = _create_valid_listener(client)

    # create 2 configs, add listener to both of them
    config1, config2 = create_two_configs_w_listener(client,
                                                     listener)

    # delete the listener
    listener = client.wait_success(client.delete(listener))
    assert listener.state == 'removed'

    # verify that the mapping is gone for both configs
    validate_remove_listener(config1, listener, client)
    validate_remove_listener(config2, listener, client)


def test_lb_config_remove_w_listeners(client):
    # create a listener
    listener = _create_valid_listener(client)

    # create 2 configs, add listener to both of them
    config1, config2 = create_two_configs_w_listener(client,
                                                     listener)

    # delete the config
    config1 = client.wait_success(client.delete(config1))
    assert config1.state == 'removed'

    # verify that the mapping is gone for config 1 and exists for config2
    validate_remove_listener(config1, listener, client)
    validate_add_listener(config2, listener, client)


def test_lb_config_remove_when_assigned(client):
    config = client.create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    lb = client.create_loadBalancer(name='lb',
                                    loadBalancerConfigId=config.id)
    client.wait_success(lb)

    with pytest.raises(ApiError) as e:
        client.wait_success(client.delete(config))

    assert e.value.error.status == 409
    assert e.value.error.code == 'LoadBalancerConfigIsInUse'


def test_lb_config_remove_when_used_by_removed_lb(client):
    config = client.create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    lb = client.create_loadBalancer(name=random_str(),
                                    loadBalancerConfigId=config.id)
    lb = client.wait_success(lb)

    client.wait_success(client.delete(lb))

    config = client.wait_success(client.delete(config))

    assert config.state == "removed"


def test_lb_config_set_listeners(client):
    config, listener1 = _create_config_and_listener(client)
    listener2 = _create_valid_listener(client)

    # set 2 listeners
    config = config.setlisteners(
        loadBalancerListenerIds=[listener1.id, listener2.id])

    validate_add_listener(config, listener1, client)
    validate_add_listener(config, listener2, client)

    # # set 1 listener
    config = config.setlisteners(loadBalancerListenerIds=[listener1.id])

    validate_remove_listener(config, listener2, client)
    validate_add_listener(config, listener1, client)

    # set 0 listener
    config = config.setlisteners(loadBalancerListenerIds=[])
    validate_remove_listener(config, listener1, client)


def _create_valid_listener(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='8080',
                                                  targetPort='80',
                                                  sourceProtocol='http',
                                                  targetProtocol='tcp')
    listener = client.wait_success(listener)
    return listener


def _create_config_and_listener(client):
    # create listener
    listener = _create_valid_listener(client)
    # create config
    config = client.create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)
    return config, listener


def test_lb_config_add_listener_twice(client):
    config, listener = _create_config_and_listener(client)

    # add listener to the config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config, listener, client)

    with pytest.raises(ApiError) as e:
        config.addlistener(loadBalancerListenerId=listener.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'loadBalancerListenerId'


def test_lb_config_remove_invalid_listener(client):
    config, listener = _create_config_and_listener(client)

    # remove non-existing listener
    with pytest.raises(ApiError) as e:
        config. \
            removelistener(loadBalancerListenerId=listener.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'loadBalancerListenerId'


def test_lb_config_add_conflicting_listener(client):
    config, listener = _create_config_and_listener(client)

    # add listener to the config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config, listener, client)

    # create duplicated listener, and try to add it to the config
    listener = _create_valid_listener(client)
    with pytest.raises(ApiError) as e:
        config.addlistener(loadBalancerListenerId=listener.id)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'sourcePort'


def test_lb_config_create_w_healthCheck(client):
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html"}
    config = client.create_loadBalancerConfig(name=random_str(),
                                              healthCheck=health_check)
    config = client.wait_success(config)

    assert config.state == 'active'
    assert config.healthCheck.name == "check1"
    assert config.healthCheck.responseTimeout == 3
    assert config.healthCheck.interval == 4
    assert config.healthCheck.healthyThreshold == 5
    assert config.healthCheck.unhealthyThreshold == 6
    assert config.healthCheck.requestLine == "index.html"


def test_lb_config_create_disable_health_check(client):
    health_check = {"name": "policy1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html"}
    config = client.create_loadBalancerConfig(name=random_str(),
                                              healthCheck=health_check)
    config = client.wait_success(config)
    assert config.healthCheck is not None

    config = client.update(config, healthCheck=None)
    config = client.wait_success(config)
    assert config.healthCheck is None


def test_lb_config_create_update_health_check(client):
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html"}
    config = client.create_loadBalancerConfig(name=random_str(),
                                              healthCheck=health_check)
    config = client.wait_success(config)
    assert config.healthCheck.name == "check1"

    health_check = {"name": "check2", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html"}

    config = client.update(config, healthCheck=health_check)
    config = client.wait_success(config)
    assert config.healthCheck.name == "check2"


def test_lb_config_create_w_app_policy(client):
    app_policy = {"name": "policy1", "cookie": "cookie1",
                  "maxLength": 4, "prefix": "true",
                  "requestLearn": "false", "timeout": 10,
                  "mode": "query_string"}

    config = client.\
        create_loadBalancerConfig(name=random_str(),
                                  appCookieStickinessPolicy=app_policy)
    config = client.wait_success(config)

    assert config.state == 'active'
    assert config.appCookieStickinessPolicy.name == "policy1"
    assert config.appCookieStickinessPolicy.cookie == "cookie1"
    assert config.appCookieStickinessPolicy.maxLength == 4
    assert config.appCookieStickinessPolicy.prefix is True
    assert config.appCookieStickinessPolicy.requestLearn is False
    assert config.appCookieStickinessPolicy.timeout == 10
    assert config.appCookieStickinessPolicy.mode == "query_string"

    # disable policy
    config = client.\
        create_loadBalancerConfig(name=random_str(),
                                  appCookieStickinessPolicy=None)
    config = client.wait_success(config)
    assert config.appCookieStickinessPolicy is None


def test_lb_config_create_w_lb_olicy(client):
    lb_policy = {"name": "policy2", "cookie": "cookie1",
                 "domain": ".test.com", "indirect": "true",
                 "nocache": "true", "postonly": "true",
                 "mode": "insert"}

    config = client.\
        create_loadBalancerConfig(name=random_str(),
                                  lbCookieStickinessPolicy=lb_policy)
    config = client.wait_success(config)

    assert config.state == 'active'
    assert config.lbCookieStickinessPolicy.name == "policy2"
    assert config.lbCookieStickinessPolicy.cookie == "cookie1"
    assert config.lbCookieStickinessPolicy.domain == ".test.com"
    assert config.lbCookieStickinessPolicy.indirect is True
    assert config.lbCookieStickinessPolicy.nocache is True
    assert config.lbCookieStickinessPolicy.postonly is True
    assert config.lbCookieStickinessPolicy.mode == "insert"

    # disable policy
    config = client.\
        create_loadBalancerConfig(name=random_str(),
                                  lbCookieStickinessPolicy=None)
    config = client.wait_success(config)
    assert config.lbCookieStickinessPolicy is None


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'
