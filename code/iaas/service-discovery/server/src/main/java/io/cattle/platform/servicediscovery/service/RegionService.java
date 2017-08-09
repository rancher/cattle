package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;

public interface RegionService {

    public void reconcileExternalLinks(long accountId);

    boolean reconcileAgentExternalCredentials(Agent agent, Account account);

    boolean deactivateAndRemoveExtenralAgent(Agent agent);

    void createExternalAccountLink(AccountLink link);

    void deleteExternalAccountLink(AccountLink link);
}
