package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

/*
 * Triggers selector containers use case. When container joins service, serviceExposeMap gets created
 */
public class ServiceExposeMapCreateNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {

    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof ServiceExposeMap)) {
            return null;
        }

        ServiceExposeMap map = (ServiceExposeMap) obj;
        return super.getNicPerVnetForAccount(map.getAccountId());
    }

}
