package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.ProcessInstance;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Field;
import org.jooq.impl.UpdatableRecordImpl;

public class ResourceIdOutputFilter implements ResourceOutputFilter {

    public static final String RESOURCE_ID_FIELD = "resource_id";
    public static final String RESOURCE_TYPE_FIELD = "resource_type";
    public static final String RESOURCE_ID = "resourceId";

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof UpdatableRecordImpl){
            UpdatableRecordImpl<?> record = (UpdatableRecordImpl<?>) original;
            for (Field<?> field: record.fields()){
                if (RESOURCE_ID_FIELD.equalsIgnoreCase(field.getName())
                        && StringUtils.isNotBlank(String.valueOf(record.getValue(RESOURCE_ID_FIELD)))){
                    converted.getFields().put(RESOURCE_ID , ApiContext.getContext()
                            .getIdFormatter().formatId(String.valueOf(record.getValue(RESOURCE_TYPE_FIELD)), record.getValue(RESOURCE_ID_FIELD)));
                    break;
                }
            }
        }
        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { AuditLog.class , ProcessInstance.class };
    }
}
