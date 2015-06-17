package io.cattle.platform.process.credential;

import io.cattle.platform.core.constants.CredentialConstants;

import javax.inject.Named;

@Named
public class AgentApiKeyCreate extends ApiKeyCreate {

    @Override
    protected String getCredentialType() {
        return CredentialConstants.KIND_AGENT_API_KEY;
    }

    @Override
    protected boolean getsHashed() {
        return false;
    }

}