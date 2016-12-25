package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.Collection;

import javax.inject.Inject;

public class SkipServiceLookup implements ServiceLookup {

    @Inject
    HostDao hostDao;
    @Inject
    ServiceDao serviceDao;


    @Override
    public Collection<? extends Service> getServices(Object obj) {
        Long accountId = null;
        if (obj instanceof Host) {
            accountId = ((Host) obj).getAccountId();
        } else if (obj instanceof Agent) {
            accountId = ((Agent) obj).getAccountId();
        }

        if (accountId == null) {
            return null;
        }

        if (hostDao.hasActiveHosts(accountId)) {
            return serviceDao.getSkipServices(accountId);
        }

        return null;
    }

}