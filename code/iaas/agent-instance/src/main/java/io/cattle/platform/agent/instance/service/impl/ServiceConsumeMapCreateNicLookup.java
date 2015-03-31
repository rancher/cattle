package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class ServiceConsumeMapCreateNicLookup extends AbstractJooqDao implements InstanceNicLookup {
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
        Service service = objectManager.loadResource(Service.class, consumeMap.getServiceId());
        
        List<? extends ServiceExposeMap> exposeMaps = mapDao.findNonRemoved(ServiceExposeMap.class, Service.class,
                service.getId());
        if (exposeMaps.isEmpty()) {
            return null;
        }

        return create().selectFrom(NIC).where(NIC.INSTANCE_ID.eq(exposeMaps.get(0).getInstanceId())).fetch();
    }
}
