package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;

import io.cattle.platform.configitem.context.dao.HealthcheckInfoDao;
import io.cattle.platform.configitem.context.data.HealthcheckData;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class HealthcheckInfoDaoImpl extends AbstractJooqDao implements HealthcheckInfoDao {

    @Inject
    ObjectManager objectMgr;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<HealthcheckData> getInstanceHealthcheckEntries(Instance instance) {
        MultiRecordMapper<HealthcheckData> mapper = new MultiRecordMapper<HealthcheckData>() {
            @Override
            protected HealthcheckData map(List<Object> input) {
                HealthcheckData data = new HealthcheckData();
                Instance targetInstance = (Instance) input.get(1);
                InstanceHealthCheck healthCheck = DataAccessor.field(targetInstance,
                        InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);
                if (healthCheck != null) {
                    data.setHealthCheck(healthCheck);
                }
                data.setTargetIpAddress((IpAddress) input.get(0));
                data.setHealthCheckUuid(((HealthcheckInstanceHostMap) input.get(2)).getUuid());

                return data;
            }
        };

        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        InstanceTable targetInstance = mapper.add(INSTANCE);
        HealthcheckInstanceHostMapTable healthcheckInstanceMap = mapper.add(HEALTHCHECK_INSTANCE_HOST_MAP);
        NicTable targetNic = NIC.as("target_nic");
        
        List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class,
                instance.getId());
        if (maps.isEmpty()) {
            return new ArrayList<>();
        }
        Long hostId = maps.get(0).getHostId();

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(targetNic)
                .on(NIC.VNET_ID.eq(targetNic.VNET_ID))
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(targetNic.INSTANCE_ID))
                .join(healthcheckInstanceMap)
                .on(healthcheckInstanceMap.HEALTHCHECK_INSTANCE_ID.eq(HEALTHCHECK_INSTANCE.ID))
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(targetNic.ID))
                .join(ipAddress)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(ipAddress.ID))
                .join(targetInstance)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(targetInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(healthcheckInstanceMap.HOST_ID.eq(hostId))
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(ipAddress.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING))
                        .and(HEALTHCHECK_INSTANCE.INSTANCE_ID.isNotNull()))
                .fetch().map(mapper);
    }

}
