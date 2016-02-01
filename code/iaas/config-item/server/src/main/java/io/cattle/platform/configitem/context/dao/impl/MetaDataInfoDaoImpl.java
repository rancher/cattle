package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.HOST_IP_ADDRESS_MAP;
import static io.cattle.platform.core.model.tables.HostTable.HOST;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;

import java.util.List;

import org.jooq.Condition;
import org.jooq.JoinType;
import org.jooq.impl.DSL;

public class MetaDataInfoDaoImpl extends AbstractJooqDao implements MetaDataInfoDao {
    @Override
    public List<ContainerMetaData> getContainersData(long accountId) {
        MultiRecordMapper<ContainerMetaData> mapper = new MultiRecordMapper<ContainerMetaData>() {
            @Override
            protected ContainerMetaData map(List<Object> input) {
                ContainerMetaData data = new ContainerMetaData();
                data.setIp((IpAddress) input.get(3));
                HostMetaData hostMetaData = null;
                if (input.get(2) != null) {
                    Host host = (Host) input.get(2);
                    IpAddress hostIpAddress = (IpAddress) input.get(0);
                    if (host != null) {
                        hostMetaData = new HostMetaData(hostIpAddress.getAddress(), host);
                    }
                }
                data.setInstanceAndHostMetadata((Instance) input.get(1), hostMetaData);

                if (input.get(4) != null) {
                    data.setExposeMap((ServiceExposeMap) input.get(4));
                }
                return data;
            }
        };

        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable instance = mapper.add(INSTANCE);
        HostTable host = mapper.add(HOST);
        IpAddressTable instanceIpAddress = mapper.add(IP_ADDRESS);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .join(INSTANCE_HOST_MAP)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .join(NIC)
                .on(NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(instanceIpAddress)
                .on(instanceIpAddress.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(instance)
                .on(instance.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(exposeMap, JoinType.LEFT_OUTER_JOIN)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .where(host.REMOVED.isNull())
                .and(instance.ACCOUNT_ID.eq(accountId))
                .and(instanceIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(exposeMap.REMOVED.isNull())
                .and(exposeMap.STATE.isNull().or(
                        exposeMap.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .orderBy(instance.ID)
                .fetch().map(mapper);
    }

    @Override
    public List<HostMetaData> getInstanceHostMetaData(long accountId, Instance instance) {
        MultiRecordMapper<HostMetaData> mapper = new MultiRecordMapper<HostMetaData>() {
            @Override
            protected HostMetaData map(List<Object> input) {

                Host host = (Host)input.get(0);
                IpAddress hostIp = (IpAddress)input.get(1);
                HostMetaData data = new HostMetaData(hostIp.getAddress(), host);
                return data;
            }
        };

        HostTable host = mapper.add(HOST);
        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);

        Condition condition = DSL.trueCondition();
        if (instance != null) {
            condition = INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.getId());
        }
            
        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .leftOuterJoin(INSTANCE_HOST_MAP)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(host.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(condition)
                .and(hostIpAddress.REMOVED.isNull())
                .and(host.ACCOUNT_ID.eq(accountId)).groupBy(host.ID)
                .fetch().map(mapper);
    }


}
