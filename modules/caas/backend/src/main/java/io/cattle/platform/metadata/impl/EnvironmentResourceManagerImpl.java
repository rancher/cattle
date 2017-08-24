package io.cattle.platform.metadata.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.*;

public class EnvironmentResourceManagerImpl implements MetadataManager {

    private static final Class<?>[] LOAD_TYPES = new Class<?>[]{
        Host.class,
        Instance.class,
        Service.class,
        Stack.class,
        Network.class,
        Account.class,
    };

    private static final Set<Class<?>> CLUSTER_TYPES = CollectionUtils.set(
        Account.class,
        Host.class,
        Network.class
    );

    ClusterDao clusterDao;
    MetadataObjectFactory factory = new MetadataObjectFactory();
    LoopManager loopManager;
    LockManager lockManager;
    ObjectManager objectManager;
    EventService eventService;
    List<Trigger> triggers;
    NullMetadata nullMetadata;

    LoadingCache<Long, Metadata> metadataCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Metadata>() {
                @Override
                public Metadata load(Long accountId) throws Exception {
                    return buildMetadata(accountId);
                }
            });

    public EnvironmentResourceManagerImpl(MetadataObjectFactory factory, LoopManager loopManager, LockManager lockManager, ObjectManager objectManager,
                                          EventService eventService, ClusterDao clusterDao, List<Trigger> triggers) {
        this.factory = factory;
        this.loopManager = loopManager;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.eventService = eventService;
        this.clusterDao = clusterDao;
        this.triggers = triggers;

        this.nullMetadata = new NullMetadata(objectManager);
    }

    private Metadata buildMetadata(long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        MetadataImpl metadata = new MetadataImpl(account, eventService, factory, loopManager, lockManager, objectManager, triggers);
        for (Class<?> clz : LOAD_TYPES) {
            List<?> objectList = Collections.emptyList();
            if (CLUSTER_TYPES.contains(clz)) {
                if (account.getClusterOwner()) {
                    if (clz == Account.class) {
                        objectList = objectManager.find(clz,
                                ObjectMetaDataManager.CLUSTER_FIELD, account.getClusterId(),
                                ObjectMetaDataManager.KIND_FIELD, Condition.in(AccountConstants.TYPE, ProjectConstants.TYPE),
                                ObjectMetaDataManager.REMOVED_FIELD, null);
                    } else {
                        objectList = objectManager.find(clz,
                                ObjectMetaDataManager.CLUSTER_FIELD, account.getClusterId(),
                                ObjectMetaDataManager.REMOVED_FIELD, null);
                    }
                }
            } else {
                objectList = objectManager.find(clz,
                        ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
                        ObjectMetaDataManager.REMOVED_FIELD, null);
            }

            objectList.forEach(metadata::changed);
        }

        return metadata;
    }

    @Override
    public Metadata getMetadataForCluster(long clusterId) {
        Long clusterAccountId = clusterDao.getOwnerAcccountIdForCluster(clusterId);
        if (clusterAccountId == null) {
            return nullMetadata;
        }
        return getMetadataForAccount(clusterAccountId);
    }

    @Override
    public Metadata getMetadataForAccount(long accountId) {
        Metadata metadata = metadataCache.getUnchecked(accountId);
        if (metadata instanceof NullMetadata) {
            metadataCache.invalidate(accountId);
        }
        return metadata;
    }

    @Override
    public List<Long> getAgentProvider(String providedServiceLabel, long clusterId) {
        return getMetadataForCluster(clusterId).getInstances().stream()
            .filter((instance) -> instance.getAgentId() != null)
            .filter(this::healthyAndActive)
            .filter((instance) -> instance.getLabels().containsKey(providedServiceLabel))
            .map(InstanceInfo::getAgentId)
            .collect(toList());
    }

    @Override
    public List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long clusterId) {
        return getMetadataForCluster(clusterId).getInstances().stream()
                .filter(instance -> instance.getAgentId() != null)
                .filter(instance -> instance.getLabels().containsKey(providedServiceLabel))
                .map(InstanceInfo::getAgentId)
                .collect(toList());
    }

    private boolean healthyAndActive(InstanceInfo instance) {
        return HealthcheckConstants.isHealthy(instance.getHealthState()) &&
                InstanceConstants.STATE_RUNNING.equals(instance.getState());

    }

}
