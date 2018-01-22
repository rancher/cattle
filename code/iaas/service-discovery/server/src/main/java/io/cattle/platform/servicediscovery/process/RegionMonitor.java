package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Region;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.AccountLinkTable.ACCOUNT_LINK;
import static io.cattle.platform.core.model.tables.RegionTable.REGION;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.servicediscovery.service.impl.RegionServiceImpl;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalProject;
import io.cattle.platform.servicediscovery.service.impl.RegionUtil.ExternalRegion;
import io.cattle.platform.task.Task;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionMonitor extends AbstractJooqDao implements Task {
    private static final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);
    private static List<String> invalidStates = Arrays.asList(CommonStatesConstants.REGISTERING, CommonStatesConstants.REMOVING, 
        CommonStatesConstants.REMOVED);

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
        List<AccountLink> accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.LINKED_REGION_ID, 
            new Condition(ConditionType.NOTNULL));
        Map<String, ExternalRegion> externalRegionMap = new HashMap<String, ExternalRegion>();
        Map<String, ExternalProject> externalProjectMap = new HashMap<String, ExternalProject>();
        for(AccountLink link : accountLinks) {
            if (invalidStates.contains(link.getState())) {
                continue;
            }
            // targetRegion not present
            Region targetRegion = regionMap.get(link.getLinkedRegionId());
            if(targetRegion == null) {
                objectManager.delete(link);
                continue;
            }
            try {
                // localRegion in targetRegion not present
                ExternalRegion externalRegion = null;
                String externalRegionKey = String.format("%s$%s", targetRegion, localRegion.getName());
                if(externalRegionMap.containsKey(externalRegionKey)) {
                    externalRegion = externalRegionMap.get(externalRegionKey);
                } else {
                    externalRegion = RegionUtil.getExternalRegion(targetRegion, localRegion.getName(), jsonMapper);
                    externalRegionMap.put(externalRegionKey, externalRegion); 
                }
                if(externalRegion == null) {
                    objectManager.delete(link);
                    continue;
                }
                // environment not present or its uuid changed 
                ExternalProject externalProject = null;
                String externalProjectKey = String.format("%s$%s", targetRegion, link.getLinkedAccount());
                if(externalProjectMap.containsKey(externalProjectKey)) {
                    externalProject = externalProjectMap.get(externalProjectKey);
                } else {
                    externalProject = RegionUtil.getTargetProjectByName(targetRegion, link.getLinkedAccount(), jsonMapper);
                    externalProjectMap.put(externalProjectKey, externalProject);
                }
                String storedUUID = DataAccessor.fieldString(link, "uuid");
                if(externalProject == null || (storedUUID!=null && !externalProject.getUuid().equals(storedUUID))) {
                    objectManager.delete(link);
                }
            } catch (Exception ex) {
                log.warn("Failed to monitor account link for %s - %s", link.getId(), ex);
            }
        }
        List<Account> accounts = objectManager.find(Account.class, ACCOUNT.KIND, AccountConstants.PROJECT_KIND, 
            ACCOUNT.REMOVED, new Condition(ConditionType.NULL));
        for(Account account : accounts) {
            try {
                regionService.reconcileExternalLinks(account.getId());
            } catch (Exception ex) {
                log.warn("Failed to reconcile external link for %s - %s", account.getId(), ex);
            }
        }
    }
    
    @Override
    public String getName() {
        return "region.monitor";
    }

}
