package io.cattle.platform.process.instance;

import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;

import io.cattle.platform.core.addon.LogConfig;
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
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

@Named
public class InstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {
    @Inject
    JsonMapper jsonMapper;

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
            List<String> dataVolumes = new ArrayList<>(DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES));
            if (!dataVolumes.contains(AgentConstants.AGENT_INSTANCE_BIND_MOUNT)) {
                dataVolumes.add(AgentConstants.AGENT_INSTANCE_BIND_MOUNT);
            }
            data.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
        }
        setName(instance, labels, data);
        setNetworkMode(instance, labels, data);
        setDns(instance, labels, data);
        setLogConfig(instance, data);

        if (!data.isEmpty()) {
            return new HandlerResult(data);
        }
        return null;
    }

    protected String toString(Object obj) {
        return ObjectUtils.toString(obj, null);
    }

    protected void setNetworkMode(Instance instance, Map<String, Object> labels, Map<Object, Object> data) {
        if (BooleanUtils.toBoolean(toString(labels.get(LABEL_RANCHER_NETWORK)))) {
            data.put(FIELD_NETWORK_MODE, DockerNetworkConstants.NETWORK_MODE_MANAGED);
        }
    }

    protected void setName(Instance instance, Map<String, Object> labels, Map<Object, Object> data) {
        String name = toString(labels.get(LABEL_DISPLAY_NAME));
        if (StringUtils.isBlank(name)) {
            return;
        }

        data.put(ObjectMetaDataManager.NAME_FIELD, name.replaceFirst("/", ""));
    }

    protected void setLogConfig(Instance instance, Map<Object, Object> data) {
        LogConfig logConfig = DataAccessor.field(instance,
                InstanceConstants.FIELD_LOG_CONFIG, jsonMapper, LogConfig.class);
        if (logConfig != null && !StringUtils.isEmpty(logConfig.getDriver()) && logConfig.getConfig() == null) {
            logConfig.setConfig(new HashMap<String, String>());
            data.put(InstanceConstants.FIELD_LOG_CONFIG, logConfig);
        }
    }

    protected boolean isRancherNetwork(Instance instance, Map<Object, Object> data) {
        String networkMode = DataAccessor.fromMap(data).withKey(FIELD_NETWORK_MODE).as(String.class);
        if (networkMode == null) {
            networkMode = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_NETWORK_MODE)
                    .as(String.class);
        }

        return DockerNetworkConstants.NETWORK_MODE_MANAGED.equals(networkMode);
    }

    protected void setDns(Instance instance, Map<String, Object> labels, Map<Object, Object> data) {
        if (instance.getSystemContainer() != null) {
            return;
        }
        if (InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            boolean addDns = DataAccessor.fromMap(labels).withKey(SystemLabels.LABEL_USE_RANCHER_DNS).withDefault(true).as(Boolean.class);
            if (addDns) {
                if (isRancherNetwork(instance, data)) {
                    List<String> dns = new ArrayList<>(DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DNS));
                    if (!dns.contains(NetworkConstants.INTERNAL_DNS_IP)) {
                        dns.add(NetworkConstants.INTERNAL_DNS_IP);
                    }
                    data.put(DockerInstanceConstants.FIELD_DNS, dns);
                    data.put(InstanceConstants.FIELD_DNS_INTERNAL, Joiner.on(",").join(dns));
                }

                List<String> dnsSearch = new ArrayList<>(DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DNS_SEARCH));
                if (!dnsSearch.contains(NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN)) {
                    dnsSearch.add(NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN);
                }
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
