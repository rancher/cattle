package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.ExternalId;

public interface ExternalIdHandler {

    public ExternalId transform(ExternalId externalId);
    
    public ExternalId untransform(ExternalId externalId);

}
