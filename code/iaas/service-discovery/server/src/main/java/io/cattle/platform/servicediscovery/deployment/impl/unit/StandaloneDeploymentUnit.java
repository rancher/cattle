package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.InstanceRevisionTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.instance.StandaloneDeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandaloneDeploymentUnit extends AbstractDeploymentUnit {

    Map<String, Map<String, Object>> instanceIdToInstanceSpec = new HashMap<>();

    public StandaloneDeploymentUnit(DeploymentUnit unit, DeploymentUnitManagerContext context) {
        super(unit, context);
        collectDeploymentUnitInstances();
        generateSidekickReferences();
    }

    @Override
    protected void createImpl() {
        // noop
    }

    @Override
    protected void collectDeploymentUnitInstances() {
        List<? extends Instance> instances = context.objectManager.find(Instance.class, INSTANCE.REMOVED, null,
                INSTANCE.DEPLOYMENT_UNIT_ID, unit.getId());
        for (Instance instance : instances) {
            String launchConfigName = instance.getId().toString();
            StandaloneDeploymentUnitInstance unitInstance = new StandaloneDeploymentUnitInstance(context,
                    instance.getName(),
                    instance,
                    launchConfigName);
            InstanceRevision revision = context.objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                    instance.getRevisionId());
            if (revision == null || !revision.getInstanceId().equals(instance.getId())) {
                continue;
            }
            instanceIdToInstanceSpec.put(instance.getId().toString(),
                    DataAccessor.fieldMap(revision, InstanceConstants.FIELD_REVISION_CONFIG));
            addDeploymentInstance(launchConfigName, unitInstance);
        }
    }

    @Override
    protected void cleanupBadAndUnhealthy() {
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                if (needToReplace(instance)) {
                    // 1. stop unhealthy instance so the ports are released
                    instance.stop();
                    instance.waitForStop();

                    // 3. create new instance
                    StandaloneDeploymentUnitInstance replacement = new StandaloneDeploymentUnitInstance(context, null,
                            null,
                            null);
                    addDeploymentInstance(replacement.getLaunchConfigName(), replacement);
                    replacement.create(getReplacementDeployParams(instance.getInstance()));
                }

                // 3. remove old instance
                removeDeploymentUnitInstance(instance, ServiceConstants.AUDIT_LOG_REMOVE_UNHEATLHY, ActivityLog.INFO);
            }
        }
    }

    public Map<String, Object> getReplacementDeployParams(Instance instance) {
        Map<String, Object> instanceSpec = instanceIdToInstanceSpec.get(instance.getId().toString());
        Map<String, Object> data = new HashMap<>();
        data.putAll(instanceSpec);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, instance.getAccountId());
        data.put(ObjectMetaDataManager.KIND_FIELD, instance.getKind());
        // fields below can be modified after the instance is created
        data.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        data.put(ObjectMetaDataManager.DESCRIPTION_FIELD, instance.getDescription());
        data.put(InstanceConstants.FIELD_PORTS,
                DataAccessor.field(instance, InstanceConstants.FIELD_PORTS, Object.class));

        data.put(InstanceConstants.FIELD_REPLACEMNT_FOR_INSTANCE_ID, instance.getId());
        data.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, instance.getDeploymentUnitId());
        data.put(InstanceConstants.FIELD_REVISION_ID, instance.getRevisionId());
        return data;
    }

    private boolean needToReplace(DeploymentUnitInstance instance) {
        // only recreate unhealthy
        Instance replacement = context.objectManager.findAny(Instance.class, INSTANCE.REMOVED, null,
                INSTANCE.SERVICE_ID, null,
                INSTANCE.REPLACEMENT_FOR, instance.getInstance().getId(), ObjectMetaDataManager.STATE_FIELD,
                new Condition(
                        ConditionType.NE, CommonStatesConstants.REMOVING));
        return replacement == null;

    }

    @Override
    protected List<DeploymentUnitInstance> getInstancesWithMismatchedIndexes() {
        return new ArrayList<>();
    }

    @Override
    public void remove(String reason, String level) {
        return;
    }

    @Override
    protected void cleanupDependencies() {
        return;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected List<String> getSidekickRefs(String launchConfigName) {
        List<String> configNames = new ArrayList<>();
        Map<String, Object> spec = instanceIdToInstanceSpec.get(launchConfigName);
        if (spec == null) {
            return new ArrayList<String>();
        }
        if (spec.containsKey(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)) {
            configNames.add(spec.get(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID).toString());
        }

        if (spec.containsKey(DockerInstanceConstants.FIELD_VOLUMES_FROM)) {
            for (Long dataVolumeContainerId : (List<Long>) spec.get(DockerInstanceConstants.FIELD_VOLUMES_FROM)) {
                configNames.add(dataVolumeContainerId.toString());
            }
        }
        return configNames;
    }


    @Override
    protected void generateSidekickReferences() {
        return;
    }

    @Override
    protected boolean startFirstOnUpgrade() {
        return false;
    }

}
