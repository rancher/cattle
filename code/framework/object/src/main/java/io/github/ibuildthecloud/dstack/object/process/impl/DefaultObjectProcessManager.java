package io.github.ibuildthecloud.dstack.object.process.impl;

import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.object.util.ObjectLaunchConfigurationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

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
        String processName = getProcessName(resource, process);
        ProcessInstance pi = createProcessInstance(processName, resource, data);
        pi.schedule();
    }

    protected String getProcessName(Object resource, StandardProcess process) {
        String type = objectManager.getType(resource);
        return type.toLowerCase() + "." + process.toString().toLowerCase();
    }

    @Override
    public ProcessInstance createProcessInstance(LaunchConfiguration config) {
        return processManager.createProcessInstance(config);
    }

    @Override
    public LaunchConfiguration createLaunchConfiguration(String processName, Object resource, Map<String, Object> data) {
        return ObjectLaunchConfigurationUtils.createConfig(schemaFactory, processName, resource, data);
    }

    @Override
    public ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data) {
        LaunchConfiguration config = createLaunchConfiguration(processName, resource, data);
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
