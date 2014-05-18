package io.cattle.platform.register.dao;

import io.cattle.platform.core.model.Agent;

import io.cattle.platform.core.model.GenericObject;

public interface RegisterDao {

    Agent createAgentForRegistration(String key, GenericObject obj);

}
