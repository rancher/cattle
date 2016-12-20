package io.cattle.platform.object.process.impl;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.Predicate;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.ObjectLaunchConfigurationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class DefaultObjectProcessManager implements ObjectProcessManager {

    ProcessManager processManager;
    SchemaFactory schemaFactory;
    ObjectManager objectManager;

    @Override
    public ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data) {
        String processName = getProcessName(resource, process);
        ProcessInstance pi = createProcessInstance(processName, resource, data);
        return pi.execute();
    }

    @Override
    public void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data) {
        scheduleStandardProcess(process, resource, data, null);
    }

    @Override
    public void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data, Predicate predicate) {
        String processName = getProcessName(resource, process);
        scheduleProcessInstance(processName, resource, data, predicate);
    }

    @Override
    public void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data, Predicate predicate) {
        LaunchConfiguration config = ObjectLaunchConfigurationUtils.createConfig(schemaFactory, processName, resource, data);
        config.setPredicate(predicate);
        processManager.scheduleProcessInstance(config);
    }

    @Override
    public void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data) {
        scheduleProcessInstance(processName, resource, data, null);
    }

    @Override
    public String getProcessName(Object resource, StandardProcess process) {
        String type = objectManager.getType(resource);
        return getStandardProcessName(process, type);
    }

    @Override
    public String getStandardProcessName(StandardProcess process, String type) {
        return type.toLowerCase() + "." + process.toString().toLowerCase();
    }

    @Override
    public String getStandardProcessName(StandardProcess process, Object obj) {
        return getProcessName(obj, process);
    }

    @Override
    public ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data) {
        LaunchConfiguration config = ObjectLaunchConfigurationUtils.createConfig(schemaFactory, processName, resource, data);
        return processManager.createProcessInstance(config);
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

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

    @Override
    public ExitReason executeProcess(String processName, Object resource, Map<String, Object> data) {
        ProcessInstance pi = createProcessInstance(processName, resource, data);
        return pi.execute();
    }

    @Override
    public void scheduleProcessInstanceAsync(final String processName, final Object resource,
            final Map<String, Object> data) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                scheduleProcessInstance(processName, resource, data);
            }
        });
    }

    @Override
    public void scheduleStandardProcessAsync(final StandardProcess process, final Object resource,
            final Map<String, Object> data) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                scheduleStandardProcess(process, resource, data);
            }
        });
    }

    @Override
    public void scheduleStandardChainedProcessAsync(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data) {
        String fromProcess = getProcessName(resource, from);
        String toProcess = getProcessName(resource, to);

        Map<String, Object> newData = new HashMap<>();
        if (data != null) {
            newData.putAll(data);
        }
        newData.put(fromProcess + ProcessLogic.CHAIN_PROCESS, toProcess);
        try {
            scheduleStandardProcessAsync(to, resource, data);
        } catch (ProcessCancelException e) {
            scheduleStandardProcessAsync(from, resource, newData);
        }
    }

}
