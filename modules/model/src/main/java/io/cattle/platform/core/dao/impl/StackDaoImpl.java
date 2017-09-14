package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.ScheduledUpgradeRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.RecordHandler;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

public class StackDaoImpl extends AbstractJooqDao implements StackDao {

    public StackDaoImpl(Configuration configuration) {
        super(configuration);
    }

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
            .fetchInto((RecordHandler<Record2<Long, Long>>) record -> {
                Long id = record.getValue(SERVICE.ID);
                Long stackId = record.getValue(SERVICE.STACK_ID);
                List<Object> list = result.get(stackId);
                if (list == null) {
                    list = new ArrayList<>();
                    result.put(stackId, list);
                }
                list.add(idFormatter.formatId(ServiceConstants.KIND_SERVICE, id));
            });
        return result;
    }

    @Override
    public List<? extends Stack> getStacksThatMatch(Collection<String> currentIds) {
        return create().select(STACK.fields())
            .from(STACK)
            .leftOuterJoin(SCHEDULED_UPGRADE)
                .on(SCHEDULED_UPGRADE.STACK_ID.eq(STACK.ID)
                        .and(SCHEDULED_UPGRADE.REMOVED.isNull())
                        .and(SCHEDULED_UPGRADE.FINISHED.isNull()))
            .where(STACK.REMOVED.isNull()
                    .and(STACK.EXTERNAL_ID.in(currentIds))
                    .and(SCHEDULED_UPGRADE.ID.isNull()))
            .fetchInto(StackRecord.class);
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
                    .and(STACK.EXTERNAL_ID.notIn(currentIds))
                    .and(SCHEDULED_UPGRADE.ID.isNull()))
            .fetchInto(StackRecord.class);
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
        Map<Long, ScheduledUpgrade> data = new HashMap<>();
        create().select(SCHEDULED_UPGRADE.fields())
                .from(SCHEDULED_UPGRADE)
                .join(STACK)
                    .on(STACK.ID.eq(SCHEDULED_UPGRADE.STACK_ID)
                            .and(STACK.REMOVE_TIME.isNotNull()))
                .where(SCHEDULED_UPGRADE.STATE.eq("scheduled"))
                .fetchInto(ScheduledUpgradeRecord.class)
                .forEach((x) -> {
                    data.put(x.getId(), x);
                });

        List<? extends ScheduledUpgrade> list = create().select(SCHEDULED_UPGRADE.fields())
                .from(SCHEDULED_UPGRADE)
                .join(STACK)
                    .on(STACK.ID.eq(SCHEDULED_UPGRADE.STACK_ID)
                            .and(STACK.STATE.in(CommonStatesConstants.ACTIVE,
                            ServiceConstants.STATE_UPGRADED)))
                .join(SERVICE)
                    .on(SERVICE.STACK_ID.eq(STACK.ID))
                .where(SCHEDULED_UPGRADE.STATE.eq("scheduled")
                        .and(accountsToIgnore.size() == 0 ? DSL.trueCondition()
                                : SCHEDULED_UPGRADE.ACCOUNT_ID.notIn(accountsToIgnore)))
                .orderBy(SCHEDULED_UPGRADE.PRIORITY.desc(), SCHEDULED_UPGRADE.CREATED.asc())
                .limit(max * 4)
                .fetchInto(ScheduledUpgradeRecord.class);

        for (ScheduledUpgrade scheduledUpgrade : list) {
            if (data.size() >= max) {
                return new ArrayList<>(data.values());
            }
            data.put(scheduledUpgrade.getId(), scheduledUpgrade);
        }

        return new ArrayList<>(data.values());
    }

    @Override
    public List<? extends Service> getServices(Long stackId) {
        if (stackId == null) {
            return Collections.emptyList();
        }
        return create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .where(SERVICE.STACK_ID.eq(stackId)
                    .and(SERVICE.REMOVED.isNull()))
                .fetchInto(ServiceRecord.class);
    }
}