package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AuditService {

    void logRequest(ApiRequest request, Policy policy);

}
