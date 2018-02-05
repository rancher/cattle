package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalProject;
import java.util.Map;

public interface RegionService {

    public void reconcileExternalLinks(long accountId);
    
    public boolean isRegionsEmpty(Agent agent, Account account, Map<String, Long> externalLinks, Map<String, ExternalProject> projects, 
            Map<Long, Region> regionsIds, Map<String, Region> regionNameToRegion);

    boolean reconcileAgentExternalCredentials(Agent agent, Account account, Map<String, Long> externalLinks, Map<String, ExternalProject> projects, 
            Map<Long, Region> regionsIds, Map<String, Region> regionNameToRegion);

    boolean deactivateAndRemoveExternalAgent(Agent agent);

    void createExternalAccountLink(AccountLink link);

    boolean deleteExternalAccountLink(AccountLink link);

}
