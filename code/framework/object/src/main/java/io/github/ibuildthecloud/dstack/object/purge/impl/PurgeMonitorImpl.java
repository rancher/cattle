package io.github.ibuildthecloud.dstack.object.purge.impl;

import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.object.purge.PurgeMonitor;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.task.Task;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurgeMonitorImpl implements PurgeMonitor, Task {

    private static final Logger log = LoggerFactory.getLogger(PurgeMonitorImpl.class);

    SchemaFactory schemaFactory;
    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;
    ObjectMetaDataManager objectMetaDataManager;
    List<String> purgeTypes = null;
    String removedState = "removed";
    ProcessManager processManager;


    @Override
    public String getName() {
        return "purge.resources";
    }

    @Override
    public void run() {
        for ( String type : findPurgableTypes() ) {
            Class<?> schemaClass = schemaFactory.getSchemaClass(type);
            if ( schemaClass == null ) {
                continue;
            }

            List<?> objects = objectManager.find(schemaClass,
                    ObjectMetaDataManager.STATE_FIELD, removedState,
                    ObjectMetaDataManager.REMOVED_FIELD, new Condition(ConditionType.NOTNULL),
                    ObjectMetaDataManager.REMOVE_TIME_FIELD, new Condition(ConditionType.LT, new Date()));

            for ( Object obj : objects ) {
                try {
                    objectProcessManager.scheduleStandardProcess(StandardProcess.PURGE, obj, null);
                    log.info("Scheduling purge for [{}] id [{}]", type, ObjectUtils.getId(obj));
                } catch ( ProcessNotFoundException e ) {
                } catch ( ProcessInstanceException e ) {
                    log.info("Failed to scheduling purge for [{}] id [{}]", type, ObjectUtils.getId(obj), e);
                }
            }
         }
    }

    protected synchronized Set<String> findPurgableTypes() {
        Set<String> types = new HashSet<String>();
        for ( Schema schema : schemaFactory.listSchemas() ) {
            while ( schema.getParent() != null ) {
                schema = schemaFactory.getSchema(schema.getParent());
            }

            String type = schema.getId();

            if ( types.contains(type) ) {
                continue;
            }

            Object fieldObj = objectMetaDataManager.convertFieldNameFor(type, ObjectMetaDataManager.REMOVED_FIELD);
            if ( fieldObj == null ) {
                /* This means there is no DB table with a removed field */
                continue;
            }

            String processName = objectProcessManager.getStandardProcessName(StandardProcess.PURGE, type);
            ProcessDefinition def = processManager.getProcessDefinition(processName);

            if ( def != null ) {
                types.add(type);
            }
        }

        return types;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public ObjectMetaDataManager getObjectMetaDataManager() {
        return objectMetaDataManager;
    }

    @Inject
    public void setObjectMetaDataManager(ObjectMetaDataManager objectMetaDataManager) {
        this.objectMetaDataManager = objectMetaDataManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public String getRemovedState() {
        return removedState;
    }

    public void setRemovedState(String removedState) {
        this.removedState = removedState;
    }

    public ObjectProcessManager getObjectProcessManager() {
        return objectProcessManager;
    }

    @Inject
    public void setObjectProcessManager(ObjectProcessManager objectProcessManager) {
        this.objectProcessManager = objectProcessManager;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

}
