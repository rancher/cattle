package io.cattle.platform.agent.instance.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.LabelTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.service.NetworkServiceInfo;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NetworkServiceProviderTable;
import io.cattle.platform.core.model.tables.NetworkServiceTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceProviderRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AgentInstanceDaoImpl extends AbstractJooqDao implements AgentInstanceDao {

    GenericResourceDao resourceDao;
    ObjectManager objectManager;

    @Override
    public Agent getAgentByUri(String uri) {
        return create()
                .selectFrom(AGENT)
                .where(AGENT.URI.eq(uri)
                        .and(AGENT.REMOVED.isNull()))
                .fetchAny();
    }

    @Override
    public Instance getInstanceByAgent(Agent agent) {
        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agent.getId())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchAny();
    }

    @Override
    public List<? extends NetworkServiceProvider> getProviders(Long networkId) {
        if ( networkId == null ) {
            return Collections.emptyList();
        }

        return create()
                .select(NETWORK_SERVICE_PROVIDER.fields())
                .from(NETWORK_SERVICE)
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE_PROVIDER.ID.eq(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID))
                .where(NETWORK_SERVICE.NETWORK_ID.eq(networkId)
                        .and(NETWORK_SERVICE_PROVIDER.KIND.eq(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE)))
                .fetchInto(NetworkServiceProviderRecord.class);
    }

    @Override
    public Instance getAgentInstance(NetworkServiceProvider provider, Nic nic) {
        if ( provider == null || nic == null || nic.getVnetId() == null ) {
            return null;
        }

        List<InstanceRecord> records = create()
                .select(INSTANCE.fields())
                    .from(INSTANCE)
                    .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                        .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                    .join(NIC)
                        .on(NIC.INSTANCE_ID.eq(INSTANCE.ID))
                    .where(NIC.VNET_ID.eq(nic.getVnetId())
                            .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID.eq(provider.getId()))
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                    .fetchInto(InstanceRecord.class);

        return records.size() > 0 ? records.get(0) : null;
    }

    @Override
    public Instance createInstanceForProvider(NetworkServiceProvider provider, Map<String, Object> properties) {
        Instance instance = resourceDao.createAndSchedule(Instance.class, properties);

        if ( provider != null ) {
            resourceDao.createAndSchedule(NetworkServiceProviderInstanceMap.class,
                    NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID, instance.getId(),
                    NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID, provider.getId());
        }

        return instance;
    }

    @Override
    public List<? extends Agent> getAgents(NetworkServiceProvider provider) {
        return create()
                .select(AGENT.fields())
                .from(AGENT)
                .join(INSTANCE)
                    .on(INSTANCE.AGENT_ID.eq(AGENT.ID))
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                    .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.REMOVED.isNull()
                        .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID.eq(provider.getId()))
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(AgentRecord.class);
    }

    @Override
    public List<? extends Credential> getActivateCredentials(Agent agent) {
        if ( agent.getAccountId() == null ) {
            return Collections.emptyList();
        }

        return create()
                .selectFrom(CREDENTIAL)
                .where(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(CREDENTIAL.ACCOUNT_ID.eq(agent.getAccountId())))
                .fetch();
    }

    @Override
    public NetworkServiceInfo getNetworkServiceInfo(long instance, String serviceKind) {
        MultiRecordMapper<NetworkServiceInfo> mapper = new MultiRecordMapper<NetworkServiceInfo>() {
            @Override
            protected NetworkServiceInfo map(List<Object> input) {
                return new NetworkServiceInfo((NetworkServiceProvider)input.get(0),
                        (NetworkService)input.get(1),
                        (Nic)input.get(2),
                        null,
                        null,
                        null);
            }
        };

        NetworkServiceProviderTable provider = mapper.add(NETWORK_SERVICE_PROVIDER);
        NetworkServiceTable networkService = mapper.add(NETWORK_SERVICE);
        NicTable nic = mapper.add(NIC);

        List<NetworkServiceInfo> infos = create()
                .select(mapper.fields())
                .from(provider)
                .join(networkService)
                    .on(provider.NETWORK_ID.eq(networkService.NETWORK_ID))
                .join(nic)
                    .on(nic.NETWORK_ID.eq(networkService.NETWORK_ID))
                .where(nic.INSTANCE_ID.eq(instance)
                        .and(networkService.KIND.eq(serviceKind))
                        .and(provider.KIND.eq(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE))
                        .and(networkService.REMOVED.isNull()))
                .fetch().map(mapper);

        return infos.size() == 0 ? null : infos.get(0);
    }

    @Override
    public void populateNicAndIp(final NetworkServiceInfo service) {
        if ( service.getAgentInstance() == null ) {
            return;
        }

        MultiRecordMapper<Object> mapper = new MultiRecordMapper<Object>() {
            @Override
            protected Object map(List<Object> input) {
                service.setAgentNic((Nic)input.get(0));
                service.setIpAddress((IpAddress)input.get(1));
                return new Object();
            }
        };

        NicTable nic = mapper.add(NIC);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);

        create()
            .select(mapper.fields())
            .from(nic)
            .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.ID))
            .join(ipAddress)
                .on(ipAddress.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
            .where(nic.INSTANCE_ID.eq(service.getAgentInstance().getId())
                    .and(nic.NETWORK_ID.eq(service.getNetworkServiceProvider().getNetworkId()))
                    .and(ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                    .and(nic.REMOVED.isNull())
                    .and(ipAddress.REMOVED.isNull())
                    .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull()))
            .fetch().map(mapper);
    }

    @Override
    public List<Long> getAgentProvider(String providedServiceLabel, long accountId) {
        return Arrays.asList(create().select(INSTANCE.AGENT_ID)
                .from(INSTANCE)
                .join(INSTANCE_LABEL_MAP)
                    .on(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(LABEL)
                    .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID).and(LABEL.KEY.eq(providedServiceLabel)))
                .where(INSTANCE.ACCOUNT_ID.eq(accountId)
                    .and(INSTANCE.AGENT_ID.isNotNull())
                    .and(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)))
                .fetch().intoArray(INSTANCE.AGENT_ID));
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}