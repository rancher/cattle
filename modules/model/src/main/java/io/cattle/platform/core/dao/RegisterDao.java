package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.GenericObject;

public interface RegisterDao {

    Agent createAgentForRegistration(String key, long clusterId, GenericObject obj);

}
