package io.github.ibuildthecloud.dstack.object.lifecycle;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessExecutionExitException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDelegateObjectLifeCycleHandler extends AbstractObjectLifeCycleHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessDelegateObjectLifeCycleHandler.class);

    ObjectProcessManager processManager;
    ObjectManager objectManager;
    SchemaFactory schemaFactory;

    @Override
    protected <T> T onCreate(T instance, Class<T> clz, Map<String, Object> properties) {
        String processName = getProcessName("create.", clz);
        LaunchConfiguration config = processManager.createLaunchConfiguration(processName, instance, properties);

        try {
            ProcessInstance process = processManager.createProcessInstance(config);
            log.info("Scheduling {}", config);
            process.schedule();

            return objectManager.loadResource(config.getResourceType(), config.getResourceId());
        } catch ( ProcessExecutionExitException e ) {
            if ( e.getExitReason() == ExitReason.FAILED_TO_ACQUIRE_LOCK ) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            } else {
                throw e;
            }
        } catch ( ProcessNotFoundException e ) {
            log.debug("Did not find process to run for [{}] due to [{}]", config, e.getMessage());
            return instance;
        }
    }

    protected String getProcessName(String prefix, Class<?> clz) {
        Schema schema = schemaFactory.getSchema(clz);

        if ( schema == null ) {
            throw new IllegalStateException("Failed to find schema for class [" + clz + "]");
        }

        String suffix = schema.getId().toLowerCase();
        return prefix + suffix;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}