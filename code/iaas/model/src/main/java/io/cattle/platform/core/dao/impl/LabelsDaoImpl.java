package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostLabelMapTable.HOST_LABEL_MAP;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.INSTANCE_LABEL_MAP;
import static io.cattle.platform.core.model.tables.LabelTable.LABEL;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

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
    
    @Override
    public Label getLabelForInstance(long instanceId, String labelKey) {
        List<Label> labels = create()
                .select(LABEL.fields())
                .from(LABEL)
                .join(INSTANCE_LABEL_MAP)
                .on(INSTANCE_LABEL_MAP.LABEL_ID.eq(LABEL.ID))
                .where(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(instanceId))
                .and(LABEL.REMOVED.isNull())
                .and(LABEL.KEY.eq(labelKey))
                .and(INSTANCE_LABEL_MAP.REMOVED.isNull())
                .fetchInto(Label.class);
        if (labels.isEmpty()) {
            return null;
        }
        return labels.get(0);
    }
}
