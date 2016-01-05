package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.iaas.healthcheck.service.HealthcheckInstancesLookup;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.List;

import javax.inject.Inject;

public class ProviderInstanceMapHealthcheckInstancesLookup extends AbstractJooqDao implements
        HealthcheckInstancesLookup {

    @Inject
    ObjectManager objectManager;

    @Inject
    ServiceDao serviceDao;

    @Override
    public List<? extends Instance> getInstances(Object obj) {
        if (!(obj instanceof NetworkServiceProviderInstanceMap)) {
            return null;
        }
        NetworkServiceProviderInstanceMap map = (NetworkServiceProviderInstanceMap) obj;
        Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
        if (instance == null) {
            return null;
        }

        return serviceDao.getInstancesWithHealtcheckEnabled(instance.getAccountId());
    }
}