package io.cattle.platform.iaas.api.credential;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class ApiKeyOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Credential) {
            Credential cred = (Credential)original;
            if (CredentialConstants.KIND_API_KEY.equals(cred.getKind()) ||
                    CredentialConstants.KIND_AGENT_API_KEY.equals(cred.getKind())) {
                converted.getLinks().put(CredentialConstants.LINK_CERTIFICATE,
                        ApiContext.getUrlBuilder().resourceLink(converted, CredentialConstants.LINK_CERTIFICATE));
            }
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] {CredentialConstants.KIND_API_KEY, CredentialConstants.TYPE};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
