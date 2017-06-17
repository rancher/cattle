package io.cattle.platform.audit;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AuditService {

    void logRequest(ApiRequest request, Policy policy);

}
