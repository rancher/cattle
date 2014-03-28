package io.cattle.platform.agent.connection.ssh.dao;

import io.cattle.platform.core.model.Agent;

import java.util.List;

public interface SshAgentDao {

    String[] getLastestActiveApiKeys(Agent agent);

    List<String[]> getClientKeyPairs();

    void saveKey(String publicPart, String privatePart);

}
