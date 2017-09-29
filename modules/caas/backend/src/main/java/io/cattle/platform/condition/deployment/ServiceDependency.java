package io.cattle.platform.condition.deployment;

import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;

public interface ServiceDependency {

    boolean satified(long accountId, long stackId, Long hostId, DependsOn dependsOn, Runnable callback);

    void setState(StackInfo stack, ServiceInfo service, InstanceInfo instance);
}
