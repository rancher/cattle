package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface DynamicSchemaDao {

    List<? extends DynamicSchema> getSchemas(long accountId, String role);

    DynamicSchema getSchema(String name, long accountId, String role);

    DynamicSchema getSchema(String name, long accountId, long serviceId);

    int deleteSchemas(long serviceId);

    List<Long> getAgentForService(Service service);

    void createRoles(DynamicSchema dynamicSchema);

    void removeRoles(DynamicSchema dynamicSchema);

    boolean isUnique(String name, List<String> roles, Long accountId);
}
