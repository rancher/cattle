package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.config.DynamicBooleanProperty;

@Named
public class ServiceMetadataInfoFactory extends AbstractAgentBaseContextFactory {

    private static DynamicBooleanProperty CACHE = ArchaiusUtil.getBoolean("cache.metadata");

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    MetaDataInfoDao metaDataInfoDao;

    Cache<String, CacheData> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();

    LoadingCache<Long, ReentrantLock> lockCache = CacheBuilder.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build(new CacheLoader<Long, ReentrantLock>() {
            @Override
            public ReentrantLock load(Long key) throws Exception {
                return new ReentrantLock();
            }
        });

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        // this method is never being called
    }

    public void writeMetadata(final Instance instance, final Callable<String> version, final OutputStream os) {
        if (instance == null) {
            return;
        }

        final InstanceHostMap hostMap = objectManager.findAny(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId());
        if (hostMap == null) {
            return;
        }

        ReentrantLock lock = doLock(instance);
        try {
            String itemVersion = version.call();
            Map<Long, HostMetaData> hostIdToHostMetadata;
            if (CACHE.get()) {
                hostIdToHostMetadata = writeCachedGenericData(instance, itemVersion, os);
            } else {
                hostIdToHostMetadata = writeGenericData(instance, os);
            }

            metaDataInfoDao.fetchSelf(hostIdToHostMetadata.get(hostMap.getHostId()), itemVersion, os);
        } catch (ExecutionException e) {
            ExceptionUtils.rethrowExpectedRuntime(e.getCause());
        } catch (Exception e) {
            ExceptionUtils.rethrowExpectedRuntime(e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
            try {
                os.close();
            } catch (IOException e) {
                ExceptionUtils.rethrowExpectedRuntime(e);
            }
        }
    }

    protected ReentrantLock doLock(final Instance instance) {
        if (!CACHE.get()) {
            return null;
        }
        ReentrantLock lock = lockCache.getUnchecked(instance.getAccountId());
        try {
            if (lock.tryLock(1, TimeUnit.MINUTES)) {
                return lock;
            }
            throw new FailedToAcquireLockException(new LockDefinition() {
                @Override
                public String getLockId() {
                    return "HOST.META." + instance.getAccountId();
                }
            });
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<Long, HostMetaData> writeCachedGenericData(final Instance instance, String itemVersion,
            OutputStream os) throws ExecutionException, IOException {
        CacheData data = cache.get(instance.getAccountId() + "/" + itemVersion, new Callable<CacheData>() {
            @Override
            public CacheData call() throws Exception {
                CacheData data = new CacheData();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                data.hostIdToHostMetadata = writeGenericData(instance, baos);
                data.bytes = baos.toByteArray();
                return data;
            }
        });

        IOUtils.write(data.bytes, os);
        return data.hostIdToHostMetadata;
    }

    private Map<Long, HostMetaData> writeGenericData(Instance instance, OutputStream os) {
        MetaHelperInfo helperInfo = fetchHelperData(objectManager.loadResource(Account.class, instance.getAccountId()));

        // Metadata visible to the user
        metaDataInfoDao.fetchContainers(helperInfo, os);
        metaDataInfoDao.fetchServices(helperInfo, os);
        metaDataInfoDao.fetchStacks(helperInfo, os);
        metaDataInfoDao.fetchHosts(helperInfo, os);
        metaDataInfoDao.fetchNetworks(helperInfo, os);

        // Helper metadata
        metaDataInfoDao.fetchServiceContainerLinks(helperInfo, os);
        metaDataInfoDao.fetchServiceLinks(helperInfo, os);
        metaDataInfoDao.fetchContainerLinks(helperInfo, os);

        return helperInfo.getHostIdToHostMetadata();
    }

    private MetaHelperInfo fetchHelperData(Account account) {
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
                metaDataInfoDao);
    }

    private static class CacheData {
        Map<Long, HostMetaData> hostIdToHostMetadata;
        byte[] bytes;
    }
}
