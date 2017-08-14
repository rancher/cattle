package io.cattle.platform.metadata.impl;

import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataModOperation;
import io.cattle.platform.metadata.model.EnvironmentInfo;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.NetworkInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.model.StackInfo;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;
import java.util.Collections;

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
    public HostInfo getHost(String uuid) {
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

}
