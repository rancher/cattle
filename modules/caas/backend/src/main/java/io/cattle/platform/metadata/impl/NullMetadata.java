package io.cattle.platform.metadata.impl;

import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.MetadataObject;
import io.cattle.platform.core.addon.metadata.NetworkInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataModOperation;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class NullMetadata implements Metadata {

    ObjectManager objectManager;

    public NullMetadata(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Collection<ServiceInfo> getServices() {
        return Collections.emptyList();
    }

    @Override
    public Collection<EnvironmentInfo> getEnvironments() {
        return Collections.emptyList();
    }

    @Override
    public Collection<HostInfo> getHosts() {
        return Collections.emptyList();
    }

    @Override
    public Collection<InstanceInfo> getInstances() {
        return Collections.emptyList();
    }

    @Override
    public Collection<StackInfo> getStacks() {
        return Collections.emptyList();
    }

    @Override
    public Collection<NetworkInfo> getNetworks() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, MetadataObject> getAll() {
        return Collections.emptyMap();
    }

    @Override
    public HostInfo getHost(String uuid) {
        return null;
    }

    @Override
    public HostInfo getHostByNodeName(String nodeName) {
        return null;
    }

    @Override
    public void remove(String uuid) {
    }

    @Override
    public void changed(Object obj) {
    }

    @Override
    public <T> T modify(Class<T> clz, long id, MetadataModOperation<T> operation) {
        return operation.modify(objectManager.loadResource(clz, id));
    }

    @Override
    public Long getClusterId() {
        return null;
    }

}
