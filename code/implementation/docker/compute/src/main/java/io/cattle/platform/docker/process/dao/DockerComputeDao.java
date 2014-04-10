package io.cattle.platform.docker.process.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

public interface DockerComputeDao {

    IpAddress getDockerIp(String ipAddress, Instance instance);

}
