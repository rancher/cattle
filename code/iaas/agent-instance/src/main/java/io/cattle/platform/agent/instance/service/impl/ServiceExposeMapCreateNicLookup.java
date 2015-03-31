package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class ServiceExposeMapCreateNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof ServiceExposeMap)) {
            return null;
        }

        ServiceExposeMap map = (ServiceExposeMap) obj;
        return create().selectFrom(NIC).where(NIC.INSTANCE_ID.eq(map.getInstanceId())).fetch();
    }

}
