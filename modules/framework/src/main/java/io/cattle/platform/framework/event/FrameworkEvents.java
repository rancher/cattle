package io.cattle.platform.framework.event;

public class FrameworkEvents {

    public static final String API_CHANGE = "api.change";
    public static final String API_EXCEPTION = "api.exception";
    public static final String STATE_CHANGE = "state.change";
    public static final String SERVICE_EVENT = "service.event";
    public static final String RESOURCE_PROGRESS = "resource.progress";
    public static final String PING = "ping";
    public static final String EXECUTE_TASK = "execute.task";
    public static final String INSPECT = "compute.instance.inspect";

    public static final String ACCOUNT_QUALIFIER = "account";

    public static final String EVENT_SEP = ";";
    public static final String REPLY_PREFIX = "reply.";

    public static final String AGENT_REQUEST = "agent.request";
    public static final String RESOURCE_CHANGE = "resource.change";
    public static final String CLEAR_CACHE = "clear.cache";
    public static final String CONTAINER_EVENT = "container.event";

    public static final String AGENT_QUALIFIER = "agent";

    public static final String CONSOLE_ACCESS = "console.access";

    public static final String ACCOUNT_SUFFIX = EVENT_SEP + ACCOUNT_QUALIFIER + "=";
    public static final String AGENT_SUFFIX = EVENT_SEP + AGENT_QUALIFIER + "=";

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