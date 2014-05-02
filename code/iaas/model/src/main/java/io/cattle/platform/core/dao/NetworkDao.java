package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.NetworkService;

import java.util.List;

public interface NetworkDao {

    List<? extends NetworkService> getNetworkService(long instanceId, String serviceKind);

}
