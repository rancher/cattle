package io.github.ibuildthecloud.dstack.docker.storage;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class DockerExternalPoolCreateLock extends AbstractLockDefinition {

    public DockerExternalPoolCreateLock() {
        super("DOCKER.EXTERNAL.POOL.CREATE");
    }

}
