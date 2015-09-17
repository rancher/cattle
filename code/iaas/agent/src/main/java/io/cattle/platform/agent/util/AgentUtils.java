package io.cattle.platform.agent.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.netflix.config.DynamicStringListProperty;

public class AgentUtils {

    public static final DynamicStringListProperty AGENT_RESOURCES = ArchaiusUtil.getList("agent.resources");

    public static Ping newPing(Agent agent) {
        if (agent == null) {
            return null;
        }
        return newPing(agent.getId());
    }

    public static Ping newPing(long agentId) {
        Ping ping = new Ping();
        ping.setResourceType(AgentConstants.TYPE);
        ping.setResourceId(Long.toString(agentId));

        return ping;
    }

    public static Map<String, Object> getAgentAuth(Agent agent, ObjectManager objectManager) {
        Account account = objectManager.loadResource(Account.class, agent.getAccountId());
        if (account == null) {
            return null;
        }

        for (Credential cred : objectManager.children(account, Credential.class)) {
            if (CredentialConstants.KIND_AGENT_API_KEY.equals(cred.getKind()) && CommonStatesConstants.ACTIVE.equals(cred.getState())) {
                try {
                    String auth = "Basic " + Base64.encodeBase64String((cred.getPublicValue() + ":" + cred.getSecretValue()).getBytes("UTF-8"));

                    return CollectionUtils.asMap("CATTLE_AGENT_INSTANCE_AUTH", auth,
                            "CATTLE_ACCESS_KEY", cred.getPublicValue(),
                            "CATTLE_SECRET_KEY", cred.getSecretValue());
                } catch (UnsupportedEncodingException e) {
                }
            }
        }

        return null;
    }
}
