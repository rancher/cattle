package io.github.ibuildthecloud.dstack.iaas.event;

import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;

public class IaasEvents {

    public static final String AGENT_REQUEST = "agent.request";
    public static final String ACCOUNT_SUFFIX = FrameworkEvents.EVENT_SEP + "account=";
    public static final String AGENT_SUFFIX = FrameworkEvents.EVENT_SEP + "agent=";

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
}
