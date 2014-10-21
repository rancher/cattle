package io.cattle.platform.docker.process.instancehostmap;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AssignHostIpLockDefinition extends AbstractLockDefinition {

    public AssignHostIpLockDefinition(Host host) {
        super("ASSIGN.HOST.IP." + host.getId());
    }

}
