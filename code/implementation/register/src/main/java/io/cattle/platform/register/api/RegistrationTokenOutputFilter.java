package io.cattle.platform.register.api;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.config.ScopedConfig;
import io.cattle.platform.register.util.RegisterConstants;
import io.cattle.platform.register.util.RegistrationToken;
import io.cattle.platform.server.context.ServerContext;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

public class RegistrationTokenOutputFilter implements ResourceOutputFilter {

    @Inject ScopedConfig scopedConfig;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if ( ! (original instanceof Credential) ) {
            return converted;
        }

        Credential cred = (Credential)original;

        String accessKey = cred.getPublicValue();
        String secretKey = cred.getSecretValue();

        if ( accessKey != null && secretKey != null ) {
            String token = RegistrationToken.createToken(accessKey, secretKey);
            URL url = null;
            if ( ServerContext.isCustomApiHost() ) {
                try {
                    url = new URL(scopedConfig.getApiUrl(null) + "/scripts/" + token);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Invalid URL", e);
                }
            } else {
                url = ApiContext.getUrlBuilder().resourceReferenceLink("scripts", token);
            }

            converted.getFields().put("token", token);
            converted.getFields().put("registrationUrl", url.toExternalForm());
            converted.getLinks().put("registrationUrl", url);
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
