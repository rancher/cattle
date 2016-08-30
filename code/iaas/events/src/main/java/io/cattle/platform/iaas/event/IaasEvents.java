package io.cattle.platform.iaas.event;

import io.cattle.platform.framework.event.FrameworkEvents;

public class IaasEvents {

    public static final String DELEGATE_REQUEST = "delegate.request";

    public static final String AGENT_REQUEST = "agent.request";
    public static final String CONFIG_UPDATE = "config.update";
    public static final String CONFIG_UPDATED = "config.updated";
    public static final String SERVICE_UPDATE = "service.update";
    public static final String GLOBAL_SERVICE_UPDATE = "global.service.update";
    public static final String RESOURCE_CHANGE = "resource.change";
    public static final String STACK_UPDATE = "stack.update";
    public static final String CLEAR_CACHE = "clear.cache";
    public static final String HOST_ENDPOINTS_UPDATE = "host.endpoints.update";

    public static final String ACCOUNT_QUALIFIER = "account";
    public static final String AGENT_QUALIFIER = "agent";

    public static final String CONSOLE_ACCESS = "console.access";

    public static final String ACCOUNT_SUFFIX = FrameworkEvents.EVENT_SEP + ACCOUNT_QUALIFIER + "=";
    public static final String AGENT_SUFFIX = FrameworkEvents.EVENT_SEP + AGENT_QUALIFIER + "=";

    public static String appendAccount(String name, Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId can not be null");
        }

        return name + ACCOUNT_SUFFIX + accountId;
    }

    public static String appendAgent(String name, Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("agentId can not be null");
        }

        return name + AGENT_SUFFIX + agentId;
    }

}