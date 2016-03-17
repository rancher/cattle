package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

/*
 * Triggers update on service link creation (service consume map creation)
 */
public class ServiceNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        return super.getRandomNicForAccount(service.getAccountId());
    }
}
