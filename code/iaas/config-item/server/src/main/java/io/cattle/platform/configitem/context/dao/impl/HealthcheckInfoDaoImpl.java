package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceExposeHostMapTable.SERVICE_EXPOSE_HOST_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.configitem.context.dao.HealthcheckInfoDao;
import io.cattle.platform.configitem.context.data.HealthcheckData;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class HealthcheckInfoDaoImpl extends AbstractJooqDao implements HealthcheckInfoDao {

    @Inject
    ObjectManager objectMgr;

    @Override
    public List<HealthcheckData> getHealthcheckEntries(Instance instance) {
        MultiRecordMapper<HealthcheckData> mapper = new MultiRecordMapper<HealthcheckData>() {
            @Override
            protected HealthcheckData map(List<Object> input) {
                HealthcheckData data = new HealthcheckData();
                data.setService((Service) input.get(1));
                data.setTargetIpAddress((IpAddress) input.get(0));

                return data;
            }
        };

        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        ServiceTable service = mapper.add(SERVICE);
        NicTable targetNic = NIC.as("target_nic");
        InstanceTable targetInstance = INSTANCE.as("instance");
        ServiceExposeMapTable serviceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(targetNic)
                .on(NIC.VNET_ID.eq(targetNic.VNET_ID))
                .join(serviceExposeMap)
                .on(serviceExposeMap.INSTANCE_ID.eq(targetNic.INSTANCE_ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(serviceExposeMap.INSTANCE_ID))
                .join(SERVICE_EXPOSE_HOST_MAP)
                .on(SERVICE_EXPOSE_HOST_MAP.HOST_ID.eq(INSTANCE_HOST_MAP.HOST_ID).and(
                        SERVICE_EXPOSE_HOST_MAP.SERVICE_EXPOSE_MAP_ID.eq(serviceExposeMap.ID)))
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(targetNic.ID))
                .join(ipAddress)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(ipAddress.ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(service)
                .on(service.ID.eq(serviceExposeMap.SERVICE_ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(ipAddress.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(serviceExposeMap.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING)))
                .fetch().map(mapper);
    }

}
