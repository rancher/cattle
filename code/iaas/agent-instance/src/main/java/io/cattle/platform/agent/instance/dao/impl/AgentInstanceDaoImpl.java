package io.cattle.platform.agent.instance.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.LabelTable.*;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.LabelTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Record2;
import org.jooq.RecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentInstanceDaoImpl extends AbstractJooqDao implements AgentInstanceDao {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceDaoImpl.class);

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
    public List<Long> getAgentProvider(String providedServiceLabel, long accountId) {
        String priorityLabel = providedServiceLabel + ".priority";
        final LabelTable priority = LABEL.as("priority");
        final Map<Long, Integer> result = new HashMap<>();
        create().select(INSTANCE.AGENT_ID, priority.VALUE)
                .from(INSTANCE)
                .join(INSTANCE_LABEL_MAP)
                    .on(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(LABEL)
                    .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID).and(LABEL.KEY.eq(providedServiceLabel)))
                .leftOuterJoin(priority)
                    .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID).and(LABEL.KEY.eq(priorityLabel)))
                .where(INSTANCE.ACCOUNT_ID.eq(accountId)
                    .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING))
                        .and(INSTANCE.HEALTH_STATE.in(HealthcheckConstants.HEALTH_STATE_HEALTHY,
                                HealthcheckConstants.HEALTH_STATE_UPDATING_HEALTHY)))
                .orderBy(priority.VALUE.asc(), INSTANCE.AGENT_ID.asc())
        .fetchInto(new RecordHandler<Record2<Long, String>>() {
            @Override
            public void next(Record2<Long, String> record) {
                Long agentId = record.getValue(INSTANCE.AGENT_ID);
                String p = record.getValue(priority.VALUE);
                Integer pVal = 10000;
                try {
                    pVal = Integer.valueOf(p);
                } catch (Exception e) {
                    log.warn("Found bad priority label value {} for agent {}", p, agentId);
                }
                result.put(agentId, pVal);
            }
        });
        
        List<Long> returnAgentIds = new ArrayList<>(result.keySet());
        
        Collections.sort(returnAgentIds, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                
                return -1;
                //return Long.compare(d1.getCreateIndex(), d2.getCreateIndex());
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