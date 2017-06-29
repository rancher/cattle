package io.cattle.platform.iaas.api.infrastructure;

import io.cattle.platform.api.auth.Policy;

public interface InfrastructureAccessManager {
    public boolean canModifyInfrastructure(Policy policy);
}
