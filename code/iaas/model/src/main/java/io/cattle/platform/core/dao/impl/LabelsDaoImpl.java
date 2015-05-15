package io.cattle.platform.core.dao.impl;

import java.util.List;

import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import static io.cattle.platform.core.model.tables.LabelTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.HostLabelMapTable.*;

public class LabelsDaoImpl extends AbstractJooqDao implements LabelsDao {

    @Override
    public List<Label> getLabelsForInstance(Long instanceId) {
        return create()
                .select(LABEL.fields())
                .from(LABEL)
                .join(INSTANCE_LABEL_MAP)
                    .on(INSTANCE_LABEL_MAP.LABEL_ID.eq(LABEL.ID))
                .where(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(instanceId))
                    .and(LABEL.REMOVED.isNull())
                    .and(INSTANCE_LABEL_MAP.REMOVED.isNull())
                .fetchInto(Label.class);
    }

    @Override
    public List<Label> getLabelsForHost(Long hostId) {
        return create()
                .select(LABEL.fields())
                .from(LABEL)
                .join(HOST_LABEL_MAP)
                    .on(HOST_LABEL_MAP.LABEL_ID.eq(LABEL.ID))
                .where(HOST_LABEL_MAP.HOST_ID.eq(hostId))
                    .and(LABEL.REMOVED.isNull())
                    .and(HOST_LABEL_MAP.REMOVED.isNull())
                .fetchInto(Label.class);
    }

}
