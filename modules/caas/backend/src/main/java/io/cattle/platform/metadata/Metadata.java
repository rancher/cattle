package io.cattle.platform.metadata;

import io.cattle.platform.metadata.model.EnvironmentInfo;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.NetworkInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.model.StackInfo;

import java.util.Collection;

public interface Metadata {

    Collection<ServiceInfo> getServices();

    Collection<EnvironmentInfo> getEnvironments();

    Collection<HostInfo> getHosts();

    Collection<InstanceInfo> getInstances();

    Collection<StackInfo> getStacks();

    Collection<NetworkInfo> getNetworks();

    HostInfo getHost(String uuid);

    void remove(String uuid);

    void changed(Object obj);

    <T> T modify(Class<T> clz, long id, MetadataModOperation<T> operation);

}
