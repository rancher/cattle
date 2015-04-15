package io.cattle.iaas.cluster.process.lock;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ClusterLock extends AbstractBlockingLockDefintion {

    public ClusterLock(Host cluster) {
        super("CLUSTER." + cluster.getId());
    }

}
