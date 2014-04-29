package io.cattle.platform.agent.instance.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceProviderRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AgentInstanceDaoImpl extends AbstractJooqDao implements AgentInstanceDao {

    GenericResourceDao resourceDao;

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
                        .and(INSTANCE.REMOVED.isNull()))
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
                            .and(INSTANCE.REMOVED.isNull()))
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
                        .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID.eq(provider.getId())))
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

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}