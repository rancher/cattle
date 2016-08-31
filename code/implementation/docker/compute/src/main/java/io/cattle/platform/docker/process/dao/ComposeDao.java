package io.cattle.platform.docker.process.dao;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Service;

public interface ComposeDao {

    Stack getComposeProjectByName(long accountId, String name);

    Service getComposeServiceByName(long accountId, String name, String projectName);

}
