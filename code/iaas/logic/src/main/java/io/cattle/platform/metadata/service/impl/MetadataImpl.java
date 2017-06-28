package io.cattle.platform.metadata.service.impl;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.MetadataObject;
import io.cattle.platform.metadata.model.NetworkInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.model.StackInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.metadata.service.MetadataModOperation;
import io.cattle.platform.metadata.service.MetadataObjectFactory;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.lock.ResourceChangeLock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataImpl implements Metadata {

    long accountId;
    MetadataObjectFactory factory;
    LoopManager loopManager;
    String[] loopNames;
    LockManager lockManager;
    ObjectManager objectManager;

    Map<String, MetadataObject> all = new ConcurrentHashMap<>();
    Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    Map<String, HostInfo> hosts = new ConcurrentHashMap<>();
    Map<String, InstanceInfo> instances = new ConcurrentHashMap<>();
    Map<String, StackInfo> stacks = new ConcurrentHashMap<>();
    Map<String, NetworkInfo> networks = new ConcurrentHashMap<>();
    Map<Class<?>, Map<String, ?>> maps = new HashMap<>();


    public MetadataImpl(long accountId, MetadataObjectFactory factory, LoopManager loopManager, LockManager lockManager, ObjectManager objectManager,
            String... loopNames) {
        super();
        this.accountId = accountId;
        this.factory = factory;
        this.loopManager = loopManager;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.loopNames = loopNames;

        maps.put(ServiceInfo.class, services);
        maps.put(HostInfo.class, hosts);
        maps.put(InstanceInfo.class, instances);
        maps.put(StackInfo.class, stacks);
        maps.put(NetworkInfo.class, networks);
    }

    @Override
    public Collection<ServiceInfo> getServices() {
        return services.values();
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
    public synchronized void remove(String uuid) {
        MetadataObject obj = all.remove(uuid);
        if (obj == null) {
            return;
        }

        for (Map<?, ?> map : maps.values()) {
            map.remove(uuid);
        }

        trigger();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void changed(Object obj) {
        MetadataObject metadataObject = factory.convert(obj);
        if (metadataObject == null) {
            return;
        }

        if (ObjectUtils.getRemoved(obj) != null) {
            remove(metadataObject.getUuid());
            return;
        }

        Map<String, ?> map = maps.get(obj.getClass());
        if (map == null) {
            return;
        }

        synchronized (this) {
            boolean trigger = false;
            Object existing = map.get(metadataObject.getUuid());
            if (existing == null) {
                trigger = true;
            } else if (!existing.equals(metadataObject)) {
                trigger = true;
            }

            if (trigger) {
                ((Map<String, Object>) map).put(metadataObject.getUuid(), metadataObject);
                trigger();
            }
        }
    }

    protected void trigger() {
        for (String loopName : loopNames) {
            loopManager.kick(loopName, AccountConstants.TYPE, accountId, null);
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
            changed(obj);
            return obj;
        });
    }

}