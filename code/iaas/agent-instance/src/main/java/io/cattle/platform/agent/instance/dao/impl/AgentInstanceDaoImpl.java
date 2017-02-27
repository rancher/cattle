package io.cattle.platform.agent.instance.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.LabelTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Record2;
import org.jooq.RecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentInstanceDaoImpl extends AbstractJooqDao implements AgentInstanceDao {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceDaoImpl.class);

    GenericResourceDao resourceDao;
    
    @Inject
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
    public Instance getInstanceByAgent(Long agentId) {
        if (agentId == null) {
            return null;
        }

        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agentId)
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchAny();
    }

    @Override
    public boolean areAllCredentialsActive(Agent agent) {
        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);

        if (agent.getAccountId() == null) {
            return false;
        }

        Set<Long> accountIds = new HashSet<>();
        accountIds.add(agent.getAccountId());

        for (Long aId : authedRoleAccountIds) {
            accountIds.add(aId);
        }

        List<? extends Credential> creds = create()
                .selectFrom(CREDENTIAL)
                .where(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(CREDENTIAL.ACCOUNT_ID.in(accountIds)))
                .fetch();

        Set<Long> credAccountIds = new HashSet<>();
        for (Credential cred : creds) {
            credAccountIds.add(cred.getAccountId());
        }

        return accountIds.equals(credAccountIds);
    }

    @Override
    public List<Long> getAgentProvider(String providedServiceLabel, long accountId) {
        final Map<Long, Integer> result = new HashMap<>();
        create().select(INSTANCE.AGENT_ID, LABEL.VALUE)
                .from(INSTANCE)
                .join(INSTANCE_LABEL_MAP)
                    .on(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(LABEL)
                    .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID).and(LABEL.KEY.eq(providedServiceLabel)))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(HOST)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .where(INSTANCE.ACCOUNT_ID.eq(accountId)
                    .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING))
                        .and(INSTANCE.HEALTH_STATE.in(HealthcheckConstants.HEALTH_STATE_HEALTHY,
                                HealthcheckConstants.HEALTH_STATE_UPDATING_HEALTHY)))
                        .and(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
                .orderBy(INSTANCE.AGENT_ID.asc())
        .fetchInto(new RecordHandler<Record2<Long, String>>() {
            @Override
            public void next(Record2<Long, String> record) {
                Long agentId = record.getValue(INSTANCE.AGENT_ID);
                String p = record.getValue(LABEL.VALUE);
                Integer pVal = 1;
                try {
                    pVal = Integer.valueOf(p);
                } catch (Exception e) {
                    log.debug("Priority value not found for agent {}. Set it as default", agentId);
                }
                result.put(agentId, pVal);
            }
        });
        
        List<Long> returnAgentIds = new ArrayList<>(result.keySet());
        
        Collections.sort(returnAgentIds, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                return result.get(id1).compareTo(result.get(id2));
            }
        });
        
        return returnAgentIds;
        
    }

    @Override
    public List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long accountId) {
        return Arrays.asList(create().select(INSTANCE.AGENT_ID)
                .from(INSTANCE)
                .join(INSTANCE_LABEL_MAP)
                    .on(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(LABEL)
                    .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID).and(LABEL.KEY.eq(providedServiceLabel)))
                .where(INSTANCE.ACCOUNT_ID.eq(accountId)
                        .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(INSTANCE.SYSTEM.isTrue())
                        .and(INSTANCE.STATE.in(InstanceConstants.STATE_RUNNING, InstanceConstants.STATE_STARTING)))
                .orderBy(INSTANCE.AGENT_ID.asc())
                .fetch().intoArray(INSTANCE.AGENT_ID));
    }
}