package io.cattle.platform.process.dynamicschema;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DynamicSchemaCreate extends AbstractDefaultProcessHandler {

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DynamicSchema dynamicSchema = (DynamicSchema) state.getResource();
        dynamicSchemaDao.createRoles(dynamicSchema);
        return null;
    }
}
