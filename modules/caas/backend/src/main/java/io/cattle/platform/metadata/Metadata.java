package io.cattle.platform.metadata;

import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.MetadataObject;
import io.cattle.platform.core.addon.metadata.NetworkInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;

import java.util.Collection;
import java.util.Map;

public interface Metadata {

    Collection<ServiceInfo> getServices();

    Collection<EnvironmentInfo> getEnvironments();

    Collection<HostInfo> getHosts();

    Collection<InstanceInfo> getInstances();

    Collection<StackInfo> getStacks();

    Collection<NetworkInfo> getNetworks();

    Map<String, MetadataObject> getAll();

    HostInfo getHost(String uuid);

    HostInfo getHostByNodeName(String nodeName);

    void remove(String uuid);

    void changed(Object obj);

    <T> T modify(Class<T> clz, long id, MetadataModOperation<T> operation);

    Long getClusterId();

    boolean isClusterOwner();

}
