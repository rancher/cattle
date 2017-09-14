package io.cattle.platform.object.process.impl;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.ObjectLaunchConfigurationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultObjectProcessManager implements ObjectProcessManager {

    ProcessManager processManager;
    SchemaFactory schemaFactory;
    ObjectManager objectManager;

    public DefaultObjectProcessManager(ProcessManager processManager, SchemaFactory schemaFactory, ObjectManager objectManager) {
        super();
        this.processManager = processManager;
        this.schemaFactory = schemaFactory;
        this.objectManager = objectManager;
    }

    @Override
    public ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data) {
        String processName = getProcessName(resource, process);
        ProcessInstance pi = createProcessInstance(processName, resource, data);
        return pi.execute();
    }

    @Override
    public void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data) {
        String processName = getProcessName(resource, process);
        scheduleProcessInstance(processName, resource, data);
    }

    @Override
    public void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data) {
        LaunchConfiguration config = ObjectLaunchConfigurationUtils.createConfig(schemaFactory, processName, resource, data);
        processManager.scheduleProcessInstance(config);
    }

    @Override
    public String getProcessName(Object resource, StandardProcess process) {
        String type = objectManager.getType(resource);
        return getStandardProcessName(process, type);
    }

    @Override
    public String getStandardProcessName(StandardProcess process, String type) {
        if (type == null || process == null) {
            return null;
        }
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
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                scheduleStandardChainedProcess(from, to, resource, data);
            }
        });
    }

    @Override
    public void scheduleStandardChainedProcess(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data) {
        String fromProcess = getProcessName(resource, from);
        String toProcess = getProcessName(resource, to);

        Map<String, Object> newData = new HashMap<>();
        if (data != null) {
            newData.putAll(data);
        }
        newData.put(fromProcess + ProcessHandler.CHAIN_PROCESS, toProcess);
        try {
            scheduleStandardProcess(to, resource, data);
        } catch (ProcessCancelException e) {
            scheduleStandardProcess(from, resource, newData);
        }
    }

    @Override
    public String getProcessName(Object resource, String processName) {
        String type = objectManager.getType(resource);
        return String.format("%s.%s", type, processName).toLowerCase();
    }

}
