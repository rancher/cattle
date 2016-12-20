package io.cattle.platform.object.purge.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.purge.RemoveMonitor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.task.Task;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class RemoveMonitorImpl implements RemoveMonitor, Task {

    @Inject
    @Named("CoreSchemaFactory")
    SchemaFactory schemaFactory;

    @Inject
    ObjectMetaDataManager objectMetaDataManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ProcessManager processManager;

    @Inject
    ObjectManager objectManager;

    private static final String ERROR_STATE = "error";
    private static final Logger log = LoggerFactory.getLogger(RemoveMonitorImpl.class);
    private static final DynamicLongProperty REMOVE_DELAY = ArchaiusUtil.getLong("remove.resources.after");


    @Override
    public void run() {
        if (!ProcessEngineUtils.enabled()) {
            return;
        }

        for (String type : findRemovableTypes()) {
            Class<?> schemaClass = schemaFactory.getSchemaClass(type);
            if (schemaClass == null) {
                continue;
            }

            List<?> objects = objectManager.find(schemaClass, ObjectMetaDataManager.STATE_FIELD, ERROR_STATE,
                    ObjectMetaDataManager.CREATED_FIELD,
                    new Condition(ConditionType.NOTNULL), ObjectMetaDataManager.CREATED_FIELD, new Condition(
                            ConditionType.LT, new Date(System.currentTimeMillis() - REMOVE_DELAY.get() * 1000)));

            for (Object obj : objects) {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("errorState", true);
                    objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, obj, data);
                    log.info("Scheduling remove for [{}] id [{}]", type, ObjectUtils.getId(obj));
                } catch (ProcessNotFoundException e) {
                } catch (ProcessInstanceException e) {
                    log.info("Failed to scheduling remove for [{}] id [{}]", type, ObjectUtils.getId(obj), e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "remove.resources";
    }

    protected synchronized Set<String> findRemovableTypes() {
        Set<String> types = new HashSet<String>();
        for (Schema schema : schemaFactory.listSchemas()) {
            while (schema.getParent() != null) {
                schema = schemaFactory.getSchema(schema.getParent());
            }

            String type = schema.getId();

            if (types.contains(type)) {
                continue;
            }

            // restricting it to instance for now
            if (!type.equalsIgnoreCase("instance")) {
                continue;
            }

            Object stateField = objectMetaDataManager.convertFieldNameFor(type, ObjectMetaDataManager.STATE_FIELD);
            if (stateField == null) {
                continue;
            }

            String processName = objectProcessManager.getStandardProcessName(StandardProcess.REMOVE, type);
            ProcessDefinition def = processManager.getProcessDefinition(processName);

            if (def != null) {
                types.add(type);
            }
        }

        return types;
    }
}
