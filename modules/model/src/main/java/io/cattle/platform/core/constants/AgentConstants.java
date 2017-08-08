package io.cattle.platform.core.constants;

import io.cattle.platform.core.model.Agent;

public class AgentConstants {

    public static final String TYPE = "agent";
    public static final String USER = "user";
    public static final String STATE_RECONNECTING = "reconnecting";
    public static final String STATE_DISCONNECTED = "disconnected";
    public static final String STATE_DISCONNECTING = "disconnecting";
    public static final String STATE_FINISHING_RECONNECT = "finishing-reconnect";
    public static final String STATE_RECONNECTED = "reconnected";
    public static final String ID_REF = "agentId";

    public static final String FIELD_ACCOUNT_ID = "accountId";
    public static final String FIELD_AUTHORIZED_ROLE_ACCOUNTS = "authorizedRoleAccountIds";

    public static final String DATA_REQUESTED_ROLES = "requestedRoles";

    public static final String PROCESS_RECONNECT = "agent.reconnect";
    public static final String PROCESS_DECONNECT = "agent.disconnect";

    public static final String AGENT_INSTANCE_BIND_MOUNT = "/var/lib/rancher/etc:/var/lib/rancher/etc:ro";
    public static final String[] AGENT_IGNORE_PREFIXES = new String[] {
            "delegate://",
            "event:///instanceId",
            "test://"
    };

    public static final String ENVIRONMENT_ROLE = "environment";
    public static final String ENVIRONMENT_ADMIN_ROLE = "environmentAdmin";
    public static final String SYSTEM_ROLE = "system";

    public static final String REPORTED_UUID = "reportedUuid";

    public static String defaultUuid(Agent agent, Class<?> type) {
        return String.format("%s-%s", agent.getUuid(), type.getSimpleName().substring(0, 1).toLowerCase());
    }
}
