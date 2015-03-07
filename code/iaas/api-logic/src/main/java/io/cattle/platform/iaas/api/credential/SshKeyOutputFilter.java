package io.cattle.platform.iaas.api.credential;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class SshKeyOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Credential) {
            if (((Credential) original).getSecretValue() != null) {
                converted.getLinks().put(CredentialConstants.LINK_PEM_FILE,
                        ApiContext.getUrlBuilder().resourceLink(converted, CredentialConstants.LINK_PEM_FILE));
            }
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { "sshKey" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
