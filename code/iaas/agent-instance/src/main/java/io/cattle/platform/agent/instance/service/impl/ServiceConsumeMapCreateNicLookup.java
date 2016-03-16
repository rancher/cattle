package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class ServiceConsumeMapCreateNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof ServiceConsumeMap)) {
            return null;
        }

        ServiceConsumeMap consumeMap = (ServiceConsumeMap) obj;
        return super.getRandomNicForAccount(consumeMap.getAccountId());
    }
}
