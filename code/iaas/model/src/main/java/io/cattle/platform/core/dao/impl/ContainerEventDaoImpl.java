package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ContainerEventTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ContainerEventConstants;
import io.cattle.platform.core.dao.ContainerEventDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooq.Record1;
import org.jooq.impl.DSL;

import com.netflix.config.DynamicIntProperty;

@Named
public class ContainerEventDaoImpl extends AbstractJooqDao implements ContainerEventDao {

    private static final DynamicIntProperty MAX_EVENTS = ArchaiusUtil.getInt("container.event.max.size");

    @Inject
    GenericResourceDao resourceDao;

    @Override
    public boolean canCreate(Long hostId, String event) {
        if (!ContainerEventConstants.EVENT_START.equals(event)) {
            return true;
        }

        Record1<Integer> count = create().select(DSL.count())
            .from(CONTAINER_EVENT)
            .where(CONTAINER_EVENT.HOST_ID.eq(hostId)
                    .and(CONTAINER_EVENT.STATE.notEqual(CommonStatesConstants.CREATED)))
            .fetchAny();
        return count.value1() < MAX_EVENTS.get();
    }

    @Override
    public boolean createContainerEvent(final ContainerEvent event, final Map<String, Object> data) {
        Long hostId = event.getHostId();
        if (hostId != null && !canCreate(hostId, event.getExternalStatus())) {
            return false;
        }

        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                resourceDao.createAndSchedule(event, data);
            }
        });

        return true;
    }

}
