package io.github.ibuildthecloud.dstack.core.event;

public class CoreEvents {

    public static final String AGENT_REQUEST = "agent.request";
    public static final String API_CHANGE = "api.change";
    public static final String API_EXCEPTION = "api.exception";
    public static final String PING = "ping";

    public static final String EVENT_SEP = ";";
    public static final String ACCOUNT_SUFFIX = EVENT_SEP + "account=";
    public static final String AGENT_SUFFIX = EVENT_SEP + "agent=";

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
