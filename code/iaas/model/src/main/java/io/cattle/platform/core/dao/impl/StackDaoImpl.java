package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.ScheduledUpgradeRecord;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooq.Record2;
import org.jooq.RecordHandler;
import org.jooq.impl.DSL;

@Named
public class StackDaoImpl extends AbstractJooqDao implements StackDao {

	 @Inject
	    ObjectManager objectManager;

	    @Override
	    public Stack getStackByExternalId(Long accountId, String externalId) {
	        return create().selectFrom(STACK)
	                .where(STACK.ACCOUNT_ID.eq(accountId))
	                .and(STACK.REMOVED.isNull())
	                .and(STACK.EXTERNAL_ID.eq(externalId))
	                .fetchAny();
	    }

        @Override
        public Map<Long, List<Object>> getServicesForStack(List<Long> ids, final IdFormatter idFormatter) {
            final Map<Long, List<Object>> result = new HashMap<>();
            create().select(SERVICE.ID, SERVICE.STACK_ID)
                .from(SERVICE)
                .where(SERVICE.STACK_ID.in(ids)
                        .and(SERVICE.REMOVED.isNull()))
                .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                    @Override
                    public void next(Record2<Long, Long> record) {
                        Long id = record.getValue(SERVICE.ID);
                        Long stackId = record.getValue(SERVICE.STACK_ID);
                        List<Object> list = result.get(stackId);
                        if (list == null) {
                            list = new ArrayList<>();
                            result.put(stackId, list);
                        }
                        list.add(idFormatter.formatId(ServiceConstants.KIND_SERVICE, id));
                    }
                });
            return result;
        }

        @Override
        public List<? extends Stack> getStacksToUpgrade(Collection<String> currentIds) {
            return create().select(STACK.fields())
                .from(STACK)
                .leftOuterJoin(SCHEDULED_UPGRADE)
                    .on(SCHEDULED_UPGRADE.STACK_ID.eq(STACK.ID)
                            .and(SCHEDULED_UPGRADE.REMOVED.isNull())
                            .and(SCHEDULED_UPGRADE.FINISHED.isNull()))
                .where(STACK.REMOVED.isNull()
                        .and(STACK.SYSTEM.isTrue())
                        .and(STACK.STATE.in(CommonStatesConstants.ACTIVE,
                                ServiceConstants.STATE_UPGRADED))
                        .and(STACK.EXTERNAL_ID.notIn(currentIds))
                        .and(STACK.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY))
                        .and(SCHEDULED_UPGRADE.ID.isNull()))
                .fetchInto(StackRecord.class);
        }

        @Override
        public boolean hasSkipServices(long stackId) {
            return null != create().select(SERVICE.ID)
                    .from(SERVICE)
                    .where(SERVICE.STACK_ID.eq(stackId)
                            .and(SERVICE.REMOVED.isNull())
                            .and(SERVICE.SKIP.isTrue()))
                    .fetchAny();
        }

        @Override
        public List<? extends ScheduledUpgrade> getRunningUpgrades() {
            return create().select(SCHEDULED_UPGRADE.fields())
                .from(SCHEDULED_UPGRADE)
                .where(SCHEDULED_UPGRADE.STATE.eq("running"))
                .fetchInto(ScheduledUpgradeRecord.class);
        }

        @Override
        public List<? extends ScheduledUpgrade> getReadyUpgrades(Set<Long> accountsToIgnore, int max) {
            return create().select(SCHEDULED_UPGRADE.fields())
                    .from(SCHEDULED_UPGRADE)
                    .where(SCHEDULED_UPGRADE.STATE.eq("scheduled")
                            .and(accountsToIgnore.size() == 0 ? DSL.trueCondition()
                                    : SCHEDULED_UPGRADE.ACCOUNT_ID.notIn(accountsToIgnore)))
                    .orderBy(SCHEDULED_UPGRADE.PRIORITY.desc(), SCHEDULED_UPGRADE.CREATED.asc())
                    .limit(max)
                    .fetchInto(ScheduledUpgradeRecord.class);
        }
}
