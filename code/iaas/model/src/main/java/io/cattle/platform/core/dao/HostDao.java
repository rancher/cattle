package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;

import java.util.List;

public interface HostDao {

    List<? extends Host> getHosts(Long accountId, List<String> uuids);

    List<? extends Host> getActiveHosts(Long accountId);
}
