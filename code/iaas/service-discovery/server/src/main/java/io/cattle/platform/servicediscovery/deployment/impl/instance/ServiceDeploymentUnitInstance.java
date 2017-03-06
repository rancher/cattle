package io.cattle.platform.servicediscovery.deployment.impl.instance;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
    public void create(Map<String, Object> deployParams) {
        if (this.instance == null) {
            Map<String, Object> launchConfigData = populateLaunchConfigData(deployParams);
            removeNamedVolumesFromLaunchConfig(deployParams, launchConfigData);
            Pair<Instance, ServiceExposeMap> instanceMapPair = createServiceInstance(
                    launchConfigData, service);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
            this.generateAuditLog(AuditEventType.create,
                    ServiceConstants.AUDIT_LOG_CREATE_EXTRA, ActivityLog.INFO);
        }
        setStartOnFailure();
    }

    @SuppressWarnings("unchecked")
    private void removeNamedVolumesFromLaunchConfig(Map<String, Object> deployParams,
            Map<String, Object> launchConfigData) {
        // remove named volumes from the list
        if (launchConfigData.get(InstanceConstants.FIELD_DATA_VOLUMES) != null) {
            List<String> dataVolumes = new ArrayList<>();
            dataVolumes.addAll((List<String>) launchConfigData.get(InstanceConstants.FIELD_DATA_VOLUMES));
            if (deployParams.get(ServiceConstants.FIELD_INTERNAL_VOLUMES) != null) {
                for (String namedVolume : (List<String>) deployParams
                        .get(ServiceConstants.FIELD_INTERNAL_VOLUMES)) {
                    dataVolumes.remove(namedVolume);
                }
                launchConfigData.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
            }
            launchConfigData.remove(ServiceConstants.FIELD_INTERNAL_VOLUMES);
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

    @SuppressWarnings("unchecked")
    protected Map<String, Object> populateLaunchConfigData(Map<String, Object> deployParams) {
        Map<String, Object> launchConfigData = context.sdService.buildServiceInstanceLaunchData(service,
                deployParams, launchConfigName, context.allocatorService);
        launchConfigData.put("name", this.instanceName);
        Object labels = launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String overrideHostName = ((Map<String, String>) labels)
                    .get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME);
            if (StringUtils.equalsIgnoreCase(overrideHostName, "container_name")) {
                String domainName = (String) launchConfigData.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
                String overrideName = getOverrideHostName(domainName, this.instanceName);
                launchConfigData.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
            }
        }

        return launchConfigData;
    }

    private String getOverrideHostName(String domainName, String instanceName) {
        String overrideName = instanceName;
        if (instanceName != null && instanceName.length() > 64) {
            // legacy code - to support old data where service suffix object wasn't created
            String serviceSuffix = ServiceConstants.getServiceSuffixFromInstanceName(instanceName);
            String serviceSuffixWDivider = instanceName.substring(instanceName.lastIndexOf(serviceSuffix) - 1);
            int truncateIndex = 64 - serviceSuffixWDivider.length();
            if (domainName != null) {
                truncateIndex = truncateIndex - domainName.length() - 1;
            }
            overrideName = instanceName.substring(0, truncateIndex) + serviceSuffixWDivider;
        }
        return overrideName;
    }



    protected Pair<Instance, ServiceExposeMap> createServiceInstance(final Map<String, Object> properties,
            final Service service) {
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
    public void removeImpl() {
        if (exposeMap != null) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, exposeMap, null);
        }
    }

    @Override
    public boolean isRestartAlways() {
        Object policyObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                DockerInstanceConstants.FIELD_RESTART_POLICY);
        if (policyObj == null) {
            return true;
        }

        RestartPolicy policy = context.jsonMapper.convertValue(policyObj, RestartPolicy.class);
        return RESTART_ALWAYS_POLICY_NAMES.contains(policy.getName());
    }
}
