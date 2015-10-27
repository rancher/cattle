package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface DynamicSchemaDao {

    List<? extends DynamicSchema> getSchemas(long accountId);

    DynamicSchema getSchema(String name, long accountId);

    DynamicSchema getSchema(String name, long accountId, long serviceId);

    int deleteSchemas(long serviceId);

    List<Long> getAgentForService(Service service);

}
