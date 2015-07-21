package io.cattle.platform.agent.connection.ssh.dao;

import io.cattle.platform.core.model.Agent;

import java.util.List;

public interface KeyPairDao {

    String[] getLastestActiveApiKeys(Agent agent);

    List<String[]> getClientKeyPairs();

    void saveKey(String publicPart, String privatePart);

}
