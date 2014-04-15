package io.cattle.platform.agent.util;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.object.ObjectManager;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

public class AgentUtils {

    public static Ping newPing(Agent agent) {
        if ( agent == null ) {
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

    public static String getAgentAuth(Agent agent, ObjectManager objectManager) {
        Account account = objectManager.loadResource(Account.class, agent.getAccountId());
        if ( account == null ) {
            return null;
        }

        for ( Credential cred : objectManager.children(account, Credential.class) ) {
            if ( CredentialConstants.KIND_API_KEY.equals(cred.getKind()) && CommonStatesConstants.ACTIVE.equals(cred.getState()) ) {
                try {
                    return "Basic " + Base64.encodeBase64String((cred.getPublicValue() + ":" + cred.getSecretValue()).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            }
        }

        return null;
    }
}
