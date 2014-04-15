package io.cattle.platform.iaas.event;

import io.cattle.platform.framework.event.FrameworkEvents;

public class IaasEvents {

    public static final String DELEGATE_REQUEST = "delegate.request";

    public static final String AGENT_REQUEST = "agent.request";
    public static final String AGENT_CLOSE = "agent.close";
    public static final String CONFIG_UPDATE = "config.update";
    public static final String RESOURCE_CHANGE = "resource.change";

    public static final String ACCOUNT_QUALIFIER = "account";
    public static final String AGENT_QUALIFIER = "agent";
    public static final String AGENT_GROUP_QUALIFIER = "agent.group";


    public static final String ACCOUNT_SUFFIX = FrameworkEvents.EVENT_SEP + ACCOUNT_QUALIFIER + "=";
    public static final String AGENT_SUFFIX = FrameworkEvents.EVENT_SEP + AGENT_QUALIFIER + "=";
    public static final String AGENT_GROUP_SUFFIX = FrameworkEvents.EVENT_SEP + AGENT_GROUP_QUALIFIER + "=";


    public static String appendAccount(String name, Long accountId) {
        if ( accountId == null ) {
            throw new IllegalArgumentException("accountId can not be null");
        }

        return name + ACCOUNT_SUFFIX + accountId;
    }

    public static String appendAgent(String name, Long agentId) {
        if ( agentId == null ) {
            throw new IllegalArgumentException("agentId can not be null");
        }

        return name + AGENT_SUFFIX + agentId;
    }

    public static String appendAgentGroup(String name, Long agentGroupId) {
        if ( agentGroupId == null ) {
            throw new IllegalArgumentException("agentGroupId can not be null");
        }

        return name + AGENT_GROUP_SUFFIX + agentGroupId;
    }
}
