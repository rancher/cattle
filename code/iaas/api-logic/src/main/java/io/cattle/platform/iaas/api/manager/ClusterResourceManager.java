package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.ClusterHostMapTable;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.core.model.tables.records.InstanceHostMapRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.MapRelationship;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class ClusterResourceManager extends AbstractJooqResourceManager {

    @Override
    public String[] getTypes() {
        return new String[] { "cluster" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Host.class };
    }

    @Override
    protected Object getMapLink(String fromType, String id, MapRelationship rel, ApiRequest request) {
        if (rel.getObjectType() != InstanceRecord.class) {
            return super.getMapLink(fromType, id, rel, request);
        }

        SchemaFactory schemaFactory = request.getSchemaFactory();
        String type = schemaFactory.getSchemaName(rel.getObjectType());

        Map<Table<?>, Condition> joins = new LinkedHashMap<Table<?>, Condition>();
        Map<Object, Object> criteria = new LinkedHashMap<Object, Object>();

        InstanceHostMapTable instanceHostMap = (InstanceHostMapTable)JooqUtils.getTable(schemaFactory, InstanceHostMapRecord.class);
        ClusterHostMapTable clusterHostMap = (ClusterHostMapTable)JooqUtils.getTable(schemaFactory, ClusterHostMapRecord.class);

        TableField<?, Object> instanceId =
                JooqUtils.getTableField(getMetaDataManager(), "instance", InstanceTable.INSTANCE.ID);
        TableField<?, Object> ihmInstanceId =
                JooqUtils.getTableField(getMetaDataManager(), "instanceHostMap", InstanceHostMapTable.INSTANCE_HOST_MAP.INSTANCE_ID);
        TableField<?, Object> ihmRemoved =
                JooqUtils.getTableField(getMetaDataManager(), "instanceHostMap", InstanceHostMapTable.INSTANCE_HOST_MAP.REMOVED);
        TableField<?, Object> ihmHostId =
                JooqUtils.getTableField(getMetaDataManager(), "instanceHostMap", InstanceHostMapTable.INSTANCE_HOST_MAP.HOST_ID);

        TableField<?, Object> chmHostId =
                JooqUtils.getTableField(getMetaDataManager(), "clusterHostMap", ClusterHostMapTable.CLUSTER_HOST_MAP.HOST_ID);
        TableField<?, Object> chmRemoved =
                JooqUtils.getTableField(getMetaDataManager(), "clusterHostMap", ClusterHostMapTable.CLUSTER_HOST_MAP.REMOVED);
        TableField<?, Object> clusterId =
                JooqUtils.getTableField(getMetaDataManager(), "clusterHostMap", ClusterHostMapTable.CLUSTER_HOST_MAP.CLUSTER_ID);

        Condition cond1 = instanceId.eq(ihmInstanceId).and(ihmRemoved == null ? DSL.trueCondition() : ihmRemoved.isNull());
        Condition cond2 = ihmHostId.eq(chmHostId).and(chmRemoved == null ? DSL.trueCondition() : chmRemoved.isNull());

        joins.put(instanceHostMap, cond1);
        joins.put(clusterHostMap, cond2);
        criteria.put(Condition.class, clusterId.eq(id));

        return listInternal(schemaFactory, type, criteria, new ListOptions(request), joins);
    }
}
