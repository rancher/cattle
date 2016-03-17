package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceIndexTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.JoinType;
import org.jooq.Record1;
import org.jooq.RecordHandler;

public class MetaDataInfoDaoImpl extends AbstractJooqDao implements MetaDataInfoDao {

    @Inject
    DnsInfoDao dnsInfoDao;

    @Override
    public List<ContainerMetaData> getContainersData(long accountId) {

        final Map<Long, IpAddress> instanceIdToHostIpMap = dnsInfoDao
                .getInstanceWithHostNetworkingToIpMap(accountId);

        MultiRecordMapper<ContainerMetaData> mapper = new MultiRecordMapper<ContainerMetaData>() {
            @Override
            protected ContainerMetaData map(List<Object> input) {
                ContainerMetaData data = new ContainerMetaData();

                Instance instance = (Instance) input.get(1);

                if (instanceIdToHostIpMap != null && instanceIdToHostIpMap.containsKey(instance.getId())) {
                    data.setIp((IpAddress) instanceIdToHostIpMap.get(instance.getId()));
                } else {
                    data.setIp((IpAddress) input.get(3));
                }

                HostMetaData hostMetaData = null;
                if (input.get(2) != null) {
                    Host host = (Host) input.get(2);
                    IpAddress hostIpAddress = (IpAddress) input.get(0);
                    if (host != null) {
                        hostMetaData = new HostMetaData(hostIpAddress.getAddress(), host);
                    }
                }
                data.setInstanceAndHostMetadata(instance, hostMetaData);

                if (input.get(4) != null) {
                    data.setExposeMap((ServiceExposeMap) input.get(4));
                }

                if (input.get(5) != null) {
                    ServiceIndex serviceIndex = (ServiceIndex) input.get(5);
                    data.setService_index(serviceIndex.getServiceIndex());
                }

                return data;
            }
        };

        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable instance = mapper.add(INSTANCE);
        HostTable host = mapper.add(HOST);
        IpAddressTable instanceIpAddress = mapper.add(IP_ADDRESS);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        ServiceIndexTable serviceIndex = mapper.add(SERVICE_INDEX);
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
                .join(serviceIndex, JoinType.LEFT_OUTER_JOIN)
                .on(serviceIndex.ID.eq(instance.SERVICE_INDEX_ID))
                .where(host.REMOVED.isNull())
                .and(instance.ACCOUNT_ID.eq(accountId))
                .and(instanceIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(exposeMap.REMOVED.isNull())
                .and(exposeMap.STATE.isNull().or(
                        exposeMap.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .and(serviceIndex.STATE.isNull().or(
                        serviceIndex.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .orderBy(instance.ID)
                .fetch().map(mapper);
    }

    @Override
    public List<String> getPrimaryIpsOnInstanceHost(final Instance instance) {
        final List<String> ips = new ArrayList<>();
        NicTable networkInstanceNic = NIC.as("client_nic");
        NicTable userInstanceNic = NIC.as("target_nic");
        create().select(IP_ADDRESS.ADDRESS)
                .from(IP_ADDRESS)
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID))
                .join(userInstanceNic)
                .on(userInstanceNic.ID.eq(IP_ADDRESS_NIC_MAP.NIC_ID))
                .join(networkInstanceNic)
                .on(networkInstanceNic.VNET_ID.eq(userInstanceNic.VNET_ID))
                .where(networkInstanceNic.INSTANCE_ID.eq(instance.getId())
                        .and(networkInstanceNic.VNET_ID.isNotNull())
                        .and(networkInstanceNic.REMOVED.isNull())
                        .and(userInstanceNic.ID.ne(networkInstanceNic.ID))
                        .and(userInstanceNic.REMOVED.isNull())
                        .and(networkInstanceNic.REMOVED.isNull())
                        .and(IP_ADDRESS.REMOVED.isNull())
                        .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                )
                .fetchInto(new RecordHandler<Record1<String>>() {
                    @Override
                    public void next(Record1<String> record) {
                        ips.add(record.value1());
                    }
                });
        return ips;
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

        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .join(INSTANCE_HOST_MAP)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(host.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.getId()))
                .and(hostIpAddress.REMOVED.isNull())
                .and(host.ACCOUNT_ID.eq(accountId))
                .fetch().map(mapper);
    }

    @Override
    public List<HostMetaData> getAllInstanceHostMetaData(long accountId) {
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

        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .where(host.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(hostIpAddress.REMOVED.isNull())
                .and(host.ACCOUNT_ID.eq(accountId))
                .fetch().map(mapper);
    }


}
