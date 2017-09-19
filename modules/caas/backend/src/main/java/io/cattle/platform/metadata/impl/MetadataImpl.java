package io.cattle.platform.metadata.impl;

import io.cattle.platform.core.addon.Removed;
import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.MetadataObject;
import io.cattle.platform.core.addon.metadata.NetworkInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataModOperation;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.lock.ResourceChangeLock;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataImpl implements Metadata {

    long accountId;
    String environmentUuid;
    Long clusterId;
    EventService eventService;
    MetadataObjectFactory factory;
    LoopManager loopManager;
    List<Trigger> triggers;
    LockManager lockManager;
    ObjectManager objectManager;

    Map<String, MetadataObject> all = new ConcurrentHashMap<>();
    Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    Map<String, HostInfo> hosts = new ConcurrentHashMap<>();
    Map<String, InstanceInfo> instances = new ConcurrentHashMap<>();
    Map<String, StackInfo> stacks = new ConcurrentHashMap<>();
    Map<String, NetworkInfo> networks = new ConcurrentHashMap<>();
    Map<String, EnvironmentInfo> environments = new ConcurrentHashMap<>();
    Map<Class<?>, Map<String, ?>> maps = new HashMap<>();


    public MetadataImpl(Account account, EventService eventService, MetadataObjectFactory factory, LoopManager loopManager, LockManager lockManager,
                        ObjectManager objectManager, List<Trigger> triggers) {
        this.accountId = account.getId();
        this.clusterId = account.getClusterId();
        this.environmentUuid = account.getUuid();
        this.eventService = eventService;
        this.factory = factory;
        this.loopManager = loopManager;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.triggers = triggers;

        maps.put(ServiceInfo.class, services);
        maps.put(HostInfo.class, hosts);
        maps.put(InstanceInfo.class, instances);
        maps.put(StackInfo.class, stacks);
        maps.put(NetworkInfo.class, networks);
        maps.put(EnvironmentInfo.class, environments);
    }

    @Override
    public Collection<ServiceInfo> getServices() {
        return services.values();
    }

    @Override
    public Collection<EnvironmentInfo> getEnvironments() {
        return environments.values();
    }

    @Override
    public Collection<HostInfo> getHosts() {
        return hosts.values();
    }

    @Override
    public Collection<InstanceInfo> getInstances() {
        return instances.values();
    }

    @Override
    public Collection<StackInfo> getStacks() {
        return stacks.values();
    }

    @Override
    public Collection<NetworkInfo> getNetworks() {
        return networks.values();
    }

    @Override
    public Map<String, MetadataObject> getAll() {
        return all;
    }

    @Override
    public synchronized void remove(String uuid) {
        MetadataObject obj = all.remove(uuid);
        if (obj == null) {
            return;
        }

        for (Map<?, ?> map : maps.values()) {
            map.remove(uuid);
        }

        trigger(new Removed(obj));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void changed(Object obj) {
        MetadataObject metadataObject = factory.convert(obj);
        if (metadataObject == null) {
            return;
        }

        metadataObject.setEnvironmentUuid(environmentUuid);
        boolean trigger = false;
        try {
            if (ObjectUtils.getRemoved(obj) != null) {
                remove(metadataObject.getUuid());
                trigger = true;
                return;
            }

            Map<String, ?> map = maps.get(metadataObject.getClass());
            if (map == null) {
                return;
            }

            synchronized (this) {
                Object existing = map.get(metadataObject.getUuid());
                if (existing == null) {
                    trigger = true;
                } else if (!existing.equals(metadataObject)) {
                    trigger = true;
                }

                if (trigger) {
                    all.put(metadataObject.getUuid(), metadataObject);
                    ((Map<String, Object>) map).put(metadataObject.getUuid(), metadataObject);
                }
            }
        } finally {
            if (trigger) {
                trigger(metadataObject);
            }
        }
    }

    protected void trigger(Object resource) {
        for (Trigger trigger : triggers) {
            trigger.trigger(accountId, clusterId, resource, Trigger.METADATA_SOURCE);
        }
    }

    @Override
    public <T> T modify(Class<T> clz, long id, MetadataModOperation<T> operation) {
        String type = objectManager.getType(clz);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type for class [" + clz + "]");
        }
        return lockManager.lock(new ResourceChangeLock(type, id), () -> {
            T obj = objectManager.loadResource(clz, id);
            obj = operation.modify(obj);
            ObjectUtils.publishChanged(eventService, objectManager, obj);
            changed(obj);
            return obj;
        });
    }

    @Override
    public HostInfo getHost(String uuid) {
        return hosts.get(uuid);
    }

    @Override
    public HostInfo getHostByNodeName(String nodeName) {
        return hosts.values().stream().filter(h -> Objects.equals(nodeName, h.getNodeName())).findAny().orElse(null);
    }

    @Override
    public Long getClusterId() {
        return clusterId;
    }

}