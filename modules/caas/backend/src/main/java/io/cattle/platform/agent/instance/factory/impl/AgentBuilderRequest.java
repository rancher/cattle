package io.cattle.platform.agent.instance.factory.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;
import java.util.Set;

public class AgentBuilderRequest {

    Long resourceAccountId;
    String uri;
    Long clusterId;
    Set<String> requestedRoles;

    public AgentBuilderRequest(Instance instance, Set<String> roles) {
        String uriPrefix = "event";
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        Object prefix = labels.get(SystemLabels.LABEL_AGENT_URI_PREFIX);
        if (prefix != null) {
            uriPrefix = prefix.toString();
        }
        this.uri = uriPrefix + ":///instanceId=" + instance.getId();
        this.resourceAccountId = instance.getAccountId();
        this.requestedRoles = roles;
        this.clusterId = instance.getClusterId();
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getUri() {
        return uri;
    }

    public Set<String> getRequestedRoles() {
        return requestedRoles;
    }

    public Long getResourceAccountId() {
        return resourceAccountId;
    }

}
