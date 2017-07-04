package io.cattle.platform.environment.impl;

import static java.util.stream.Collectors.*;

import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.metadata.service.MetadataObjectFactory;
import io.cattle.platform.metadata.service.impl.MetadataImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EnvironmentResourceManagerImpl implements EnvironmentResourceManager {

    private static final Class<?>[] LOAD_TYPES = new Class<?>[]{
        Host.class,
        Instance.class,
        Service.class,
        Stack.class,
        Network.class
    };

    MetadataObjectFactory factory = new MetadataObjectFactory();
    LoopManager loopManager;
    LockManager lockManager;
    ObjectManager objectManager;
    EventService eventService;

    LoadingCache<Long, Metadata> metadataCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Metadata>() {
                @Override
                public Metadata load(Long accountId) throws Exception {
                    return buildMetadata(accountId);
                }
            });

    public EnvironmentResourceManagerImpl(MetadataObjectFactory factory, LoopManager loopManager, LockManager lockManager, ObjectManager objectManager,
            EventService eventService) {
        this.factory = factory;
        this.loopManager = loopManager;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.eventService = eventService;
    }

    private Metadata buildMetadata(long accountId) {
        MetadataImpl metadata = new MetadataImpl(accountId, eventService, factory, loopManager, lockManager, objectManager,
                LoopFactory.HEALTHCHECK_SCHEDULE,
                LoopFactory.HEALTHSTATE_CALCULATE,
                LoopFactory.HEALTHCHECK_CLEANUP,
                LoopFactory.SYSTEM_STACK,
                LoopFactory.ENDPOINT_UPDATE,
                LoopFactory.SERVICE_MEMBERSHIP);

        for (Class<?> clz : LOAD_TYPES) {
            for (Object obj : objectManager.find(clz,
                    ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
                    ObjectMetaDataManager.REMOVED_FIELD, null)) {
                metadata.changed(obj);
            }
        }

        return metadata;
    }

    @Override
    public Metadata getMetadata(long accountId) {
        return metadataCache.getUnchecked(accountId);
    }

    @Override
    public boolean hostHasContainerLabel(long accountId, long hostId, String labelKey, String labelValue) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends Long> getActiveHosts(long accountId) {
        return getMetadata(accountId).getHosts().stream()
                .filter((host) -> CommonStatesConstants.ACTIVE.equals(host.getState()) && CommonStatesConstants.ACTIVE.equals(host.getAgentState()))
                .map((host) -> host.getId())
                .collect(toList());
    }

    @Override
    public List<? extends Long> getHosts(long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String[]> getLabelsForHost(long accountId, long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getAgentProvider(String providedServiceLabel, long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

}
