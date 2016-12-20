package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceMetadataInfoFactory extends AbstractAgentBaseContextFactory {
    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    MetaDataInfoDao metaDataInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        // this method is never being called
    }

    public void writeMetadata(Instance instance, String itemVersion, OutputStream os) {
        if (instance == null) {
            return;
        }

        InstanceHostMap hostMap = objectManager.findAny(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId());
        if (hostMap == null) {
            return;
        }

        MetaHelperInfo helperInfo = fetchHelperData(objectManager.loadResource(Account.class, instance.getAccountId()),
                hostMap.getHostId());
        try {
            // Metadata visible to the user
            metaDataInfoDao.fetchContainers(helperInfo, os);
            metaDataInfoDao.fetchServices(helperInfo, os);
            metaDataInfoDao.fetchStacks(helperInfo, os);
            metaDataInfoDao.fetchHosts(helperInfo, os);
            metaDataInfoDao.fetchNetworks(helperInfo, os);
            metaDataInfoDao.fetchSelf(helperInfo, itemVersion, os);
            // Helper metadata
            metaDataInfoDao.fetchServiceContainerLinks(helperInfo, os);
            metaDataInfoDao.fetchServiceLinks(helperInfo, os);
            metaDataInfoDao.fetchContainerLinks(helperInfo, os);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                ExceptionUtils.rethrowExpectedRuntime(e);
            }
        }
    }

    private MetaHelperInfo fetchHelperData(Account account, long agentHostId) {
        Map<Long, Account> accounts = new HashMap<>();
        Set<Long> linkedServicesIds = new HashSet<>();
        Set<Long> linkedStackIds = new HashSet<>();
        List<? extends Account> allAccounts = objectManager.find(Account.class, ACCOUNT.REMOVED, new Condition(
                ConditionType.NULL));
        Map<Long, Account> allAccountsMap = new HashMap<>();
        for (Account a : allAccounts) {
            allAccountsMap.put(a.getId(), a);
        }
        // fetch accounts/services that are linked TO your account
        accounts.put(account.getId(), account);
        List<? extends AccountLink> accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                account.getId(), ACCOUNT_LINK.REMOVED, null);
        for (AccountLink accountLink : accountLinks) {
            Long accountId = accountLink.getLinkedAccountId();
            accounts.put(accountId, allAccountsMap.get(accountId));
        }
        
        // fetch accounts/services that your account is linked TO
        accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.LINKED_ACCOUNT_ID,
                account.getId(), ACCOUNT_LINK.REMOVED, null);
        for (AccountLink accountLink : accountLinks) {
            Long accountId = accountLink.getAccountId();
            accounts.put(accountId, allAccountsMap.get(accountId));
        }

        // fetch services linked both ways
        Map<Long, Long> map1 = consumeMapDao.findConsumedServicesIdsToStackIdsFromOtherAccounts(account.getId());
        Map<Long, Long> map2 = consumeMapDao.findConsumedByServicesIdsToStackIdsFromOtherAccounts(account.getId());
        linkedServicesIds.addAll(map1.keySet());
        linkedStackIds.addAll(map1.values());
        linkedServicesIds.addAll(map2.keySet());
        linkedStackIds.addAll(map2.values());

        return new MetaHelperInfo(account, accounts, linkedServicesIds, linkedStackIds,
                metaDataInfoDao, agentHostId);
    }
}
