package io.github.ibuildthecloud.dstack.process.base;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.process.HandlerResultListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

public class ActivateByDefaultListener implements HandlerResultListener {

    SchemaFactory schemaFactory;

    @Override
    public HandlerResult onResult(ProcessInstance instance, ProcessState state, ProcessHandler handler,
            ProcessDefinition def, HandlerResult result) {
        if ( result != null && ( result.shouldContinue() || result.shouldDelegate() ) )
            return result;

        if ( instance.getName().endsWith(".create") ) {
            if ( ArchaiusUtil.getBoolean("activate.by.default." + def.getResourceType()).get() ) {
                if ( result == null ) {
                    result = new HandlerResult();
                }
                result.shouldDelegate(true);
            }
        }

        return result;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
