package io.cattle.platform.iaas.api.auditing.dao;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.AuditLog;

import java.util.Map;

public interface AuditLogDao {
    AuditLog create(String resourceType, Long resourceId, Map<String, Object> data, Identity identity, Long accountId, Long authenticatedAsAccountId,
                    String eventType, String authType, Long runTime, String description, String clientIp);
}
