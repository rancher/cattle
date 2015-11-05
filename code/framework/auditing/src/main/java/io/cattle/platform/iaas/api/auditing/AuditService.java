package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

public interface AuditService {
    String AUDIT_LOG_KIND = "auditLog";

    void logRequest(ApiRequest request, Policy policy);

    /**
     * @param data {@link Map} -  The {@link Object}'s that will be set as the fields on the created
     *             {@link io.cattle.platform.core.model.AuditLog} using the {@link io.cattle.platform.core.model.AuditLog}.setData.
     *             Where each Key {@link String} will
     *             be a the name of the field and the {@link Object} will be the Value.
     * @param resource @see Resource
     */
    void logResourceModification(Object resource, Map<String, Object> data, AuditEventType eventType, String description, Long accountId, String clientIp);
}
