package io.github.ibuildthecloud.agent.connection.ssh.dao;

import io.github.ibuildthecloud.dstack.core.model.Agent;

import java.util.List;

public interface SshAgentDao {

    String[] getLastestActiveApiKeys(Agent agent);

    List<String[]> getClientKeyPairs();

    void saveKey(String publicPart, String privatePart);

}
