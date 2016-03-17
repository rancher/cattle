package io.cattle.platform.docker.process.dao;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;

public interface ComposeDao {

    Environment getComposeProjectByName(long accountId, String name);

    Service getComposeServiceByName(long accountId, String name, String projectName);

}
