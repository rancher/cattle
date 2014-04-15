package io.cattle.platform.iaas.network.impl;

import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.iaas.network.NetworkServiceManager;
import io.cattle.platform.iaas.network.dao.NetworkServiceDao;

import javax.inject.Inject;

public class NetworkServiceManagerImpl implements NetworkServiceManager {

    NetworkServiceDao networkServiceDao;

    @Override
    public NetworkService getService(Long networkId, String networkService) {
        return networkServiceDao.getService(networkId, networkService);
    }

    public NetworkServiceDao getNetworkServiceDao() {
        return networkServiceDao;
    }

    @Inject
    public void setNetworkServiceDao(NetworkServiceDao networkServiceDao) {
        this.networkServiceDao = networkServiceDao;
    }

}
