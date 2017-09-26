package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.K8sLifecycleManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class K8sLifecycleManagerImpl implements K8sLifecycleManager {


    @Override
    public void instanceCreate(Instance instance) {
        if (!instance.getNativeContainer()) {
            return;
        }

        Map<String, Object> labels = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS);

        Object namespace = labels.get(SystemLabels.LABEL_K8S_POD_NAMESPACE);
        Object name = labels.get(SystemLabels.LABEL_K8S_POD_NAME);
        Object containerName = labels.get(SystemLabels.LABEL_K8S_CONTAINER_NAME);
        if (namespace == null || name == null) {
            return;
        }

        // Going to modify to get non-RO instance
        labels = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS);

        labels.put(SystemLabels.LABEL_SERVICE_DEPLOYMENT_UNIT, labels.get(SystemLabels.LABEL_K8S_POD_UID));
        labels.put(SystemLabels.LABEL_STACK_NAME, namespace);

        if (SystemLabels.POD_VALUE.equals(containerName)) {
            labels.put(SystemLabels.LABEL_RANCHER_NETWORK, "true");
            labels.put(SystemLabels.LABEL_SERVICE_LAUNCH_CONFIG, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, labels.get(SystemLabels.LABEL_K8S_POD_NAME));
        } else {
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, containerName);
        }

        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);
    }

}
