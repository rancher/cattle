package io.cattle.platform.iaas.network.dao;

import io.cattle.platform.core.model.NetworkService;

public interface NetworkServiceDao {

    NetworkService getService(Long networkId, String networkService);

}
