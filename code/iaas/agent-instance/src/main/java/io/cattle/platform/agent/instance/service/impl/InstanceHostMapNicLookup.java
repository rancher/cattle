package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class InstanceHostMapNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof InstanceHostMap)) {
            return null;
        }

        InstanceHostMap map = (InstanceHostMap) obj;

        if (map.getInstanceId() == null) {
            return Collections.emptyList();
        }
        Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
        if (instance != null) {
            return super.getNicPerVnetForAccount(instance.getAccountId());
        }
        return null;
    }

}
