package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class HostIpAddressMapNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    ObjectManager objectManager;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof HostIpAddressMap)) {
            return null;
        }

        HostIpAddressMap map = (HostIpAddressMap) obj;
        Host host = objectManager.loadResource(Host.class, map.getHostId());
        if (host == null) {
            return Collections.emptyList();
        }

        if (host.getPhysicalHostId() == null) {
            return create().select(NIC.fields()).from(INSTANCE_HOST_MAP).join(NIC).on(NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID)).where(
                    INSTANCE_HOST_MAP.HOST_ID.eq(((HostIpAddressMap) obj).getHostId()).and(NIC.REMOVED.isNull()).and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                    .fetchInto(NicRecord.class);
        } else {
            return create().select(NIC.fields()).from(HOST).join(INSTANCE_HOST_MAP).on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID)).join(NIC).on(
                    NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID)).where(
                    HOST.PHYSICAL_HOST_ID.eq(host.getPhysicalHostId()).and(HOST.REMOVED.isNull()).and(NIC.REMOVED.isNull()).and(
                            INSTANCE_HOST_MAP.REMOVED.isNull())).fetchInto(NicRecord.class);
        }
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
