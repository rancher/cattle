package io.cattle.platform.process.dynamicSchema;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DynamicSchemaRemove extends AbstractDefaultProcessHandler {

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        dynamicSchemaDao.removeRoles((DynamicSchema) state.getResource());
        return null;
    }
}
