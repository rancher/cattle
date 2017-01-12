package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Map;

import javax.inject.Named;

@Named
public class K8sPreInstanceCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    public static final String POD = "POD";
    public static final String POD_UID = "io.kubernetes.pod.uid";
    public static final String POD_NAME = "io.kubernetes.pod.name";
    public static final String POD_NAMESPACE = "io.kubernetes.pod.namespace";
    public static final String CONTAINER_NAME = "io.kubernetes.container.name";

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);

        Object namespace = labels.get(POD_NAMESPACE);
        Object name = labels.get(POD_NAME);
        Object containerName = labels.get(CONTAINER_NAME);
        if (namespace == null || name == null) {
            return null;
        }

        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, labels.get(POD_UID));
        labels.put(ServiceConstants.LABEL_STACK_NAME, namespace);

        if (POD.equals(containerName)) {
            labels.put(SystemLabels.LABEL_RANCHER_NETWORK, "true");
            labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, labels.get(POD_NAME));
        } else {
            labels.put(SystemLabels.LABEL_DISPLAY_NAME, containerName);
        }

        return new HandlerResult(InstanceConstants.FIELD_LABELS, labels).withShouldContinue(true);
    }

    @Override
    public int getPriority() {
        return 0;
    }

}
