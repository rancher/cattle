package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DynamicSchema;
import java.util.List;

public interface DynamicSchemaDao {

    List<? extends DynamicSchema> getSchemas(long accountId, String role);

    DynamicSchema getSchema(String name, long accountId, String role);

    void createRoles(DynamicSchema dynamicSchema);

    void removeRoles(DynamicSchema dynamicSchema);

    boolean isUnique(String name, List<String> roles, Long accountId);
}
