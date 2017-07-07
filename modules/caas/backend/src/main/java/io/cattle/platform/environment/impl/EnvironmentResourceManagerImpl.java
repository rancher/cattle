package io.cattle.platform.environment.impl;

import static java.util.stream.Collectors.*;

import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
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
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.metadata.service.MetadataObjectFactory;
import io.cattle.platform.metadata.service.impl.MetadataImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;
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
    public boolean hostHasContainerLabel(long accountId, String hostUuid, String labelKey, String labelValue) {
        HostInfo host = getMetadata(accountId).getHost(hostUuid);
        if (host == null) {
            return false;
        }

        return Objects.equal(labelValue, host.getLabels().get(labelKey));
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
        return getMetadata(accountId).getHosts().stream()
                .map((host) -> host.getId())
                .collect(toList());
    }

    @Override
    public Map<String, String> getLabelsForHost(long accountId, String hostUuid) {
        HostInfo hostInfo = getMetadata(accountId).getHost(hostUuid);
        return hostInfo == null ? Collections.emptyMap() : hostInfo.getLabels();
    }

    @Override
    public Iterator<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs) {
        return new Iterator<HostInfo>() {
            HostInfo next;
            int orderedIndex;
            int restIndex;
            List<HostInfo> rest;
            Metadata metadata = getMetadata(options.getAccountId());
            Set<String> ignore = new HashSet<>(orderedHostUUIDs);

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public HostInfo next() {
                HostInfo result = next;
                advance();
                return result;
            }

            private void advance() {
                if (options.getRequestedHostId() != null) {
                    if (next != null) {
                        next = null;
                        return;
                    }

                    next = metadata.getHosts().stream()
                            .filter((host) -> host.getId() == options.getRequestedHostId().longValue())
                            .findFirst().orElse(null);
                    return;
                }

                for (; orderedIndex < orderedHostUUIDs.size() ; orderedIndex++) {
                    next = metadata.getHost(orderedHostUUIDs.get(orderedIndex));
                    if (!ignore.contains(next.getUuid()) && validHost(next)) {
                        orderedIndex++;
                        return;
                    }
                }

                if (rest == null) {
                    rest = new ArrayList<>(metadata.getHosts());
                    Collections.shuffle(rest);
                }

                for (; restIndex < rest.size() ; restIndex++) {
                    next = rest.get(restIndex);
                    if (validHost(next)) {
                        restIndex++;
                        return;
                    }
                }

                next = null;
            }
        };
    }

    @Override
    public List<Long> getAgentProvider(String providedServiceLabel, long accountId) {
        return getMetadata(accountId).getInstances().stream()
            .filter((instance) -> instance.getAgentId() != null)
            .filter(this::healthyAndActive)
            .filter((instance) -> instance.getLabels().containsKey(providedServiceLabel))
            .map((instance) -> instance.getId())
            .collect(toList());
    }

    private boolean healthyAndActive(InstanceInfo instance) {
        return HealthcheckConstants.HEALTH_STATE_HEALTHY.equals(instance.getHealthState()) &&
                InstanceConstants.STATE_RUNNING.equals(instance.getState());

    }

    private boolean validHost(HostInfo hostInfo) {
        if (hostInfo == null) {
            return false;
        }
        return CommonStatesConstants.ACTIVE.equals(hostInfo.getAgentState()) &&
                CommonStatesConstants.ACTIVE.equals(hostInfo.getState());
    }

}
