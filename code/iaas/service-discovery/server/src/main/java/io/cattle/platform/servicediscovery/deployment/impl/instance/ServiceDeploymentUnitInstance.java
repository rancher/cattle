package io.cattle.platform.servicediscovery.deployment.impl.instance;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.exception.DataChangedException;

public class ServiceDeploymentUnitInstance extends AbstractDeploymentUnitInstance {
    protected Service service;
    protected Stack stack;
    protected ServiceExposeMap exposeMap;

    public ServiceDeploymentUnitInstance(DeploymentUnitManagerContext context, Service service, Stack stack,
            String instanceName, Instance instance, ServiceExposeMap exposeMap, String launchConfigName) {
        super(context, instanceName, instance, launchConfigName);
        this.service = service;
        this.stack = stack;
        this.exposeMap = exposeMap;
    }

    @Override
    protected void createImpl(Map<String, Object> deployParams) {
        if (this.instance == null) {
            Pair<Instance, ServiceExposeMap> instanceMapPair = createServiceInstance(deployParams);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
            this.generateAuditLog(AuditEventType.create,
                    ServiceConstants.AUDIT_LOG_CREATE_EXTRA, ActivityLog.INFO);
        }
    }

    @Override
    public void scheduleCreate() {
        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                    null);
        }
        if (instance.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                    null);
        }
    }

    @Override
    protected void removeImpl() {
        if (exposeMap != null) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, exposeMap, null);
        }
    }

    protected Pair<Instance, ServiceExposeMap> createServiceInstance(final Map<String, Object> properties) {
        DataChangedException ex = null;
        for (int i = 0; i < 30; i++) {
            try {
                final ServiceRecord record = JooqUtils.getRecordObject(context.objectManager.loadResource(
                        Service.class,
                        service.getId()));
                return context.exposeMapDao.createServiceInstance(properties, service, record);
            } catch (DataChangedException e) {
                // retry
                ex = e;
            }
        }
        throw ex;
    }

    @Override
    protected boolean isRestartAlways() {
        RestartPolicy rp = DataAccessor.field(instance, DockerInstanceConstants.FIELD_RESTART_POLICY,
                context.jsonMapper,
                RestartPolicy.class);
        if (rp == null) {
            // to preserve pre-refactoring behavior for service
            return true;
        }
        return RESTART_ALWAYS_POLICY_NAMES.contains(rp.getName());
    }
}
