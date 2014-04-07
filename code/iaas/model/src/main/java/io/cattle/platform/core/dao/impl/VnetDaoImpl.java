package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostVnetMapTable.*;
import static io.cattle.platform.core.model.tables.SubnetVnetMapTable.*;
import io.cattle.platform.core.dao.VnetDao;
import io.cattle.platform.core.model.HostVnetMap;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.tables.records.HostVnetMapRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class VnetDaoImpl extends AbstractJooqDao implements VnetDao {

    ObjectManager objectManager;

    @Override
    public Vnet findVnetFromHosts(Long hostId, Long subnetId) {
        List<? extends HostVnetMap> vnets = create()
                .select(HOST_VNET_MAP.fields())
                .from(HOST_VNET_MAP)
                .join(SUBNET_VNET_MAP)
                    .on(SUBNET_VNET_MAP.VNET_ID.eq(HOST_VNET_MAP.VNET_ID))
                .where(SUBNET_VNET_MAP.SUBNET_ID.eq(subnetId)
                        .and(HOST_VNET_MAP.HOST_ID.eq(hostId)))
                .fetchInto(HostVnetMapRecord.class);

        if ( vnets.size() == 0 ) {
            return null;
        }

        return objectManager.loadResource(Vnet.class, vnets.get(0).getVnetId());
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
