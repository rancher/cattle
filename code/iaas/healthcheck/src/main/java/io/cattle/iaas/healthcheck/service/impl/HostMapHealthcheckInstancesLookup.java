package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.iaas.healthcheck.service.HealthcheckInstancesLookup;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HostMapHealthcheckInstancesLookup extends AbstractJooqDao implements HealthcheckInstancesLookup {

    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Instance> getInstances(Object obj) {
        if (!(obj instanceof HealthcheckInstanceHostMap)) {
            return null;
        }
        HealthcheckInstanceHostMap hostMap = (HealthcheckInstanceHostMap) obj;

        List<Instance> instances = new ArrayList<>();
        if (!(obj instanceof HealthcheckInstanceHostMap)) {
            return instances;
        }
        HealthcheckInstance hInstance = objectManager.loadResource(HealthcheckInstance.class,
                hostMap.getHealthcheckInstanceId());
        if (hInstance == null || hInstance.getRemoved() != null) {
            return instances;
        }

        Instance instance = objectManager.loadResource(Instance.class, hInstance.getInstanceId());

        if (instance != null && instance.getRemoved() == null) {
            instances.add(instance);
        }
        return instances;
    }
}