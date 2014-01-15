package io.github.ibuildthecloud.dstack.core.dao.impl;

import java.util.List;

import static io.github.ibuildthecloud.dstack.core.model.tables.InstanceHostMapTable.*;

import io.github.ibuildthecloud.dstack.core.dao.InstanceDao;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;

public class InstanceDaoImpl extends AbstractCoreDao implements InstanceDao {

    @Override
    public List<? extends InstanceHostMap> findNonRemovedInstanceHostMaps(long instanceId) {
        return create()
                .selectFrom(INSTANCE_HOST_MAP)
                .where(
                        INSTANCE_HOST_MAP.INSTANCE_ID.eq(instanceId)
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                .fetch();
    }

}
