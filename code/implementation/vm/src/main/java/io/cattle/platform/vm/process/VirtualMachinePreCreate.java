package io.cattle.platform.vm.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualMachinePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    private static final String[] CAPS = new String[] { "NET_ADMIN" };
    private static final String[] VOLUMES = new String[] { "/var/lib/rancher/vm:/vm" };
    private static final String[] DEVICES = new String[] { "/dev/kvm:/dev/kvm", "/dev/net/tun:/dev/net/tun" };

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.KIND_VIRTUAL_MACHINE.equals(instance.getKind())) {
            return null;
        }

        Map<Object, Object> fields = new HashMap<>();
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);

        labels.put(SystemLabels.LABEL_VM, "true");
        if (instance.getMemoryMb() == null) {
            labels.put(SystemLabels.LABEL_VM_MEMORY, "512");
        } else {
            labels.put(SystemLabels.LABEL_VM_MEMORY, instance.getMemoryMb().toString());
        }
        labels.put(SystemLabels.LABEL_VM_USERDATA, instance.getUserdata());
        labels.put(SystemLabels.LABEL_VM_VCPU, DataAccessor.fieldString(instance, InstanceConstants.FIELD_VCPU));

        fields.put(InstanceConstants.FIELD_LABELS, labels);
        fields.put(ObjectMetaDataManager.CAPABILITIES_FIELD, Arrays.asList("console"));

        // TODO: remove this
        Map<String, Object> env = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_ENVIRONMENT);
        env.put("IP_PREFIX", "10.42");
        fields.put(InstanceConstants.FIELD_ENVIRONMENT, env);

        List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        List<String> devices = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DEVICES);
        List<String> caps = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_CAP_ADD);

        for (String volume : VOLUMES) {
            addToList(dataVolumes, volume);
        }
        for (String device : DEVICES) {
            addToList(devices, device);
        }
        for (String cap : CAPS) {
            addToList(caps, cap);
        }

        fields.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
        fields.put(DockerInstanceConstants.FIELD_DEVICES, devices);
        fields.put(DockerInstanceConstants.FIELD_CAP_ADD, caps);

        return new HandlerResult(true, fields);
    }

    protected void addToList(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
