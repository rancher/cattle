package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.K8sLifecycleManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class K8sLifecycleManagerImpl implements K8sLifecycleManager {

    public static final String POD = "POD";
    public static final String POD_UID = "io.kubernetes.pod.uid";
    public static final String POD_NAME = "io.kubernetes.pod.name";
    public static final String POD_NAMESPACE = "io.kubernetes.pod.namespace";
    public static final String CONTAINER_NAME = "io.kubernetes.container.name";

    @Override
    public void instanceCreate(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS);

        Object namespace = labels.get(POD_NAMESPACE);
        Object name = labels.get(POD_NAME);
        Object containerName = labels.get(CONTAINER_NAME);
        if (namespace == null || name == null) {
            return;
        }

        // Going to modify to get non-RO instance
        labels = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS);

        labels.put(SystemLabels.LABEL_SERVICE_DEPLOYMENT_UNIT, labels.get(POD_UID));
        labels.put(SystemLabels.LABEL_STACK_NAME, namespace);

        if (POD.equals(containerName)) {
            labels.put(SystemLabels.LABEL_RANCHER_NETWORK, "true");
            labels.put(SystemLabels.LABEL_SERVICE_LAUNCH_CONFIG, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, labels.get(POD_NAME));
        } else {
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, containerName);
        }

        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);
    }

}
