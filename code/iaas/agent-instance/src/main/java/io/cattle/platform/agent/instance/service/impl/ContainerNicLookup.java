package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * This class is invoked on instance healthcheck changes
 *
 *
 */
public class ContainerNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance container = (Instance) obj;
        List<? extends Nic> result = create().selectFrom(NIC)
                .where(NIC.INSTANCE_ID.eq(container.getId()))
                .fetch();

        /* We don't want to return removed network agents, this creates a loop in which a new network agent will automatically be recreated */
        if (result.size() > 0 && result.get(0).getRemoved() != null
                && InstanceConstants.SYSTEM_CONTAINER_NETWORK_AGENT.equalsIgnoreCase(container.getSystemContainer())) {
            return Collections.emptyList();
        }

        return result;
    }
}
