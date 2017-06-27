package io.cattle.platform.process.dynamicschema;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

public class DynamicSchemaProcessManager {

    DynamicSchemaDao dynamicSchemaDao;

    public DynamicSchemaProcessManager(DynamicSchemaDao dynamicSchemaDao) {
        this.dynamicSchemaDao = dynamicSchemaDao;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        DynamicSchema dynamicSchema = (DynamicSchema) state.getResource();
        dynamicSchemaDao.createRoles(dynamicSchema);
        return null;
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        dynamicSchemaDao.removeRoles((DynamicSchema) state.getResource());
        return null;
    }


}
