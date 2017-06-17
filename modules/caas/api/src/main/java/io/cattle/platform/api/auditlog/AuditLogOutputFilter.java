package io.cattle.platform.api.auditlog;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.tables.records.AuditLogRecord;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import org.apache.commons.lang3.StringUtils;

public class AuditLogOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof AuditLogRecord) {
            AuditLogRecord auditLogRecord = (AuditLogRecord) original;
            if (auditLogRecord.getResourceId() != null) {
                converted.getLinks().put("resource",
                        ApiContext.getUrlBuilder().resourceReferenceLink(
                                auditLogRecord.getResourceType(), String.valueOf(auditLogRecord.getResourceId())));
            }
            if (StringUtils.isNotBlank(auditLogRecord.getAuthenticatedAsIdentityId())) {
                converted.getLinks().put("authenticatedAsIdentity",
                        ApiContext.getUrlBuilder().resourceReferenceLink(
                                Identity.class, String.valueOf(auditLogRecord.getAuthenticatedAsIdentityId())));
            }
        }
        return converted;
    }

}
