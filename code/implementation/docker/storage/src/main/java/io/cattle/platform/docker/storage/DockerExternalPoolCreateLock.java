package io.cattle.platform.docker.storage;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class DockerExternalPoolCreateLock extends AbstractLockDefinition {

    public DockerExternalPoolCreateLock() {
        super("DOCKER.EXTERNAL.POOL.CREATE");
    }

}
