package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.AccountLinkTable.ACCOUNT_LINK;
import static io.cattle.platform.core.model.tables.RegionTable.REGION;
import static io.cattle.platform.core.model.tables.AgentTable.AGENT;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.servicediscovery.service.impl.RegionServiceImpl;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalAccountLink;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalProject;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalProjectResponse;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalRegion;
import io.cattle.platform.task.Task;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionMonitor extends AbstractJooqDao implements Task{
    private static final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);
    private static List<String> invalidStates = Arrays.asList(CommonStatesConstants.REGISTERING, CommonStatesConstants.REMOVING, 
        CommonStatesConstants.REMOVED);
    private static List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED);
    AgentRemove agentRemove = new AgentRemove();

    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    RegionService regionService;

    
    @Override
    public void run() {
        List<Region> regions = objectManager.find(Region.class, REGION.REMOVED, new Condition(ConditionType.NULL));
        if (regions.size() == 0) {
            return;
        }
        Region localRegion = null; 
        Map<Long, Region> regionMap = new HashMap<Long, Region>();
        for(Region region : regions) {
            regionMap.put(region.getId(), region);
            if(region.getLocal()) {
                localRegion = region;
            }
        }
        Map<String, ExternalRegion> externalRegionMap = new HashMap<String, ExternalRegion>();
        Map<String, ExternalProjectResponse> externalProjectMap = new HashMap<String, ExternalProjectResponse>();
        HashSet<String> existingLinks = new HashSet<String>();
        
        List<AccountLink> accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.LINKED_REGION_ID, 
            new Condition(ConditionType.NOTNULL));
        
        cleanLinks(accountLinks, externalRegionMap, externalProjectMap, regionMap, existingLinks, localRegion);
        
        cleanAgents(existingLinks);
                
        List<Account> accounts = objectManager.find(Account.class, ACCOUNT.KIND, AccountConstants.PROJECT_KIND, 
            ACCOUNT.REMOVED, new Condition(ConditionType.NULL));
        for(Account account : accounts) {
            try {
                regionService.reconcileExternalLinks(account.getId());
            } catch (Exception ex) {
                log.warn(String.format("Failed to reconcile external link for %s - %s", account.getId(), ex));
            }
        }
    }

    private void cleanLinks (List<AccountLink> accountLinks, Map<String, ExternalRegion> externalRegionMap, 
            Map<String, ExternalProjectResponse> externalProjectMap, Map<Long, Region> regionMap, HashSet<String> existingLinks, 
            Region localRegion) {
        for(AccountLink link : accountLinks) {
            if (invalidStates.contains(link.getState())) {
                continue;
            }
            // targetRegion not present
            Region targetRegion = regionMap.get(link.getLinkedRegionId());
            if(targetRegion == null) {
                objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, link, null);
                continue;
            }
            try {
                // localRegion in targetRegion not present
                ExternalRegion externalRegion = null;
                String externalRegionKey = String.format("%s:%s", targetRegion.getName(), localRegion.getName());
                if(externalRegionMap.containsKey(externalRegionKey)) {
                    externalRegion = externalRegionMap.get(externalRegionKey);
                } else {
                    externalRegion = RegionUtil.getExternalRegion(targetRegion, localRegion.getName(), jsonMapper);
                    externalRegionMap.put(externalRegionKey, externalRegion); 
                }
                if(externalRegion == null) {
                        objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, link, null);
                    continue;
                }
                // environment not present or its uuid changed 
                ExternalProjectResponse externalProjectResponse = null;
                String externalProjectKey = String.format("%s:%s", targetRegion.getName(), link.getLinkedAccount());
                if(externalProjectMap.containsKey(externalProjectKey)) {
                    externalProjectResponse = externalProjectMap.get(externalProjectKey);
                } else {
                    externalProjectResponse = RegionUtil.getTargetProjectByName(targetRegion, link.getLinkedAccount(), jsonMapper);
                    externalProjectMap.put(externalProjectKey, externalProjectResponse);
                }

                String storedUUID = DataAccessor.fieldString(link, "linkedAccountUuid");
                ExternalProject externalProject = externalProjectResponse.getExternalProject();
                boolean notFound = (externalProject == null && externalProjectResponse.getStatusCode()==200);
                if(notFound) {
                        objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, link, null);
                        continue;
                } else {
                    boolean uuidsDoNotMatch = (externalProject!=null &&
                            storedUUID!=null && !storedUUID.equals(externalProject.getUuid()));
                    if(uuidsDoNotMatch) {
                            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, link, null);
                            continue;
                    }
                }
                if(link.getExternal()) {
                        Account localAccount = objectManager.loadResource(Account.class, link.getAccountId());
                        ExternalAccountLink accLink = RegionUtil.getAccountLinkForExternal(targetRegion, externalProject, localAccount, jsonMapper);
                        if(accLink == null || removedStates.contains(accLink.getState())){
                            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, link, null);
                        }
                } else {
                    existingLinks.add(externalProjectKey);
                }
            } catch (Exception ex) {
                log.warn(String.format("Failed to monitor account link for %s - %s", link.getId(), ex));
                if(!link.getExternal()) {
                        existingLinks.add(String.format("%s:%s", link.getLinkedRegion(), link.getLinkedAccount()));
                }
                return;
            }
        }
    }
    
    private void cleanAgents(HashSet<String> existingLinks) {
        List<Agent> agents = objectManager.find(Agent.class, AGENT.REMOVED, new Condition(ConditionType.NULL),
                AGENT.EXTERNAL_ID, new Condition(ConditionType.NOTNULL));
        
        if(agents.size() > 0) { 
            for(Agent agent : agents) {
                String[] uri = agent.getUri().substring(RegionUtil.EXTERNAL_AGENT_URI_PREFIX.length()).split("_");
                String regionEnv = String.format("%s:%s", uri[0], uri[1]);
                if(existingLinks.contains(regionEnv)) {
                    continue; 
                }
                agentRemove.deactivateThenRemove(agent, null);
            }
        }
    }
    
    @Override
    public String getName() {
        return "region.monitor";
    }
    
    public static class AgentRemove extends AbstractDefaultProcessHandler {
        @Override
        public HandlerResult handle(final ProcessState state, ProcessInstance process) {
                return null;
        }
        
            public ExitReason deactivateThenRemove(Object obj, Map<String, Object> data) {
                return super.deactivateThenRemove(obj, data);
            }
    }

}

