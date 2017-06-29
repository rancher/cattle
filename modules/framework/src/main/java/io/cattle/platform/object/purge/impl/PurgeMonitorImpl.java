package io.cattle.platform.object.purge.impl;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.purge.PurgeMonitor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.task.Task;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurgeMonitorImpl implements PurgeMonitor, Task {

    private static final Logger log = LoggerFactory.getLogger(PurgeMonitorImpl.class);

    SchemaFactory schemaFactory;
    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;
    ObjectMetaDataManager objectMetaDataManager;
    String removedState = "removed";
    ProcessManager processManager;

    public PurgeMonitorImpl(SchemaFactory schemaFactory, ObjectProcessManager objectProcessManager, ObjectManager objectManager,
            ObjectMetaDataManager objectMetaDataManager, ProcessManager processManager) {
        super();
        this.schemaFactory = schemaFactory;
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
        this.objectMetaDataManager = objectMetaDataManager;
        this.processManager = processManager;
    }

    @Override
    public String getName() {
        return "purge.resources";
    }

    @Override
    public void run() {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                runInternal();
                /* We only want purged to be ran as part of replay */
                DeferredUtils.resetDeferred();
            }
        });
    }

    public void runInternal() {
        for (String type : findPurgableTypes()) {
            Class<?> schemaClass = schemaFactory.getSchemaClass(type);
            if (schemaClass == null) {
                continue;
            }

            List<?> objects = objectManager.find(schemaClass, ObjectMetaDataManager.STATE_FIELD, removedState, ObjectMetaDataManager.REMOVED_FIELD,
                    new Condition(ConditionType.NOTNULL), ObjectMetaDataManager.REMOVE_TIME_FIELD, new Condition(ConditionType.LT, new Date()));

            for (Object obj : objects) {
                try {
                    objectProcessManager.scheduleStandardProcess(StandardProcess.PURGE, obj, null);
                    log.debug("Scheduling purge for [{}] id [{}]", type, ObjectUtils.getId(obj));
                } catch (ProcessNotFoundException e) {
                } catch (ProcessInstanceException e) {
                    log.info("Failed to scheduling purge for [{}] id [{}]", type, ObjectUtils.getId(obj), e);
                }
            }
        }
    }

    protected synchronized Set<String> findPurgableTypes() {
        Set<String> types = new HashSet<>();
        for (Schema schema : schemaFactory.listSchemas()) {
            while (schema.getParent() != null) {
                schema = schemaFactory.getSchema(schema.getParent());
            }

            String type = schema.getId();

            if (types.contains(type)) {
                continue;
            }

            Object fieldObj = objectMetaDataManager.convertFieldNameFor(type, ObjectMetaDataManager.REMOVED_FIELD);
            if (fieldObj == null) {
                /* This means there is no DB table with a removed field */
                continue;
            }

            String processName = objectProcessManager.getStandardProcessName(StandardProcess.PURGE, type);
            ProcessDefinition def = processManager.getProcessDefinition(processName);

            if (def != null) {
                types.add(type);
            }
        }

        return types;
    }

}
