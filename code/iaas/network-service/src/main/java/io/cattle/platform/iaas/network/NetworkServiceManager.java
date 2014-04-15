package io.cattle.platform.iaas.network;

import io.cattle.platform.core.model.NetworkService;

public interface NetworkServiceManager {

    NetworkService getService(Long networkId, String networkService);

}
