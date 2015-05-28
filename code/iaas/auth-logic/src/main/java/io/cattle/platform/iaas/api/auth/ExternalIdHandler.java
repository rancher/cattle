package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.ExternalId;

public interface ExternalIdHandler {

    ExternalId transform(ExternalId externalId);
    
    ExternalId untransform(ExternalId externalId);

}
