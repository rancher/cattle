package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.google.common.base.Joiner;

@Named
public class InstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.CONTAINER_LIKE.contains(instance.getKind())) {
            return null;
        }
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        Map<Object, Object> data = new HashMap<>();
        if (labels.containsKey(SystemLabels.LABEL_AGENT_CREATE)
                && labels.get(SystemLabels.LABEL_AGENT_CREATE).equals("true")) {
            List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
            dataVolumes.add(AgentConstants.AGENT_INSTANCE_BIND_MOUNT);
            data.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
        }
        setDns(instance, labels, data);

        if (!data.isEmpty()) {
            return new HandlerResult(data);
        }
        return null;
    }

    protected void setDns(Instance instance, Map<String, Object> labels, Map<Object, Object> data) {
        if (instance.getSystemContainer() != null) {
            return;
        }
        if (InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            boolean addDns = true;
            Object lblValue = labels.get(SystemLabels.LABEL_USE_RANCHER_DNS);
            if (lblValue != null && !Boolean.valueOf(lblValue.toString())) {
                addDns = false;
            }

            if (addDns) {
                String networkMode = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_NETWORK_MODE)
                        .as(String.class);
                if (DockerNetworkConstants.NETWORK_MODE_MANAGED.equals(networkMode)) {
                    List<String> dns = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DNS);
                    dns.add(NetworkConstants.INTERNAL_DNS_IP);
                    data.put(DockerInstanceConstants.FIELD_DNS, dns);
                    data.put(InstanceConstants.FIELD_DNS_INTERNAL, Joiner.on(",").join(dns));
                }
                List<String> dnsSearch = DataAccessor.fieldStringList(instance,
                        DockerInstanceConstants.FIELD_DNS_SEARCH);
                dnsSearch.add(NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN);
                data.put(DockerInstanceConstants.FIELD_DNS_SEARCH, dnsSearch);
                data.put(InstanceConstants.FIELD_DNS_SEARCH_INTERNAL, Joiner.on(",").join(dnsSearch));
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
