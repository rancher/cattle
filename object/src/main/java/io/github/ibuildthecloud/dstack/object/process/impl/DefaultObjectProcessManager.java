package io.github.ibuildthecloud.dstack.object.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.utils.ObjectLaunchConfigurationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.Map;

import javax.inject.Inject;

public class DefaultObjectProcessManager implements ObjectProcessManager {

    ProcessManager processManager;
    SchemaFactory schemaFactory;

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

}
