package io.cattle.platform.iaas.api.auditing.dao.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.tables.AuditLogTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.api.auditing.dao.AuditLogDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;


public class AuditLogDaoImpl extends AbstractJooqDao implements AuditLogDao {

    @Inject
    ObjectManager objectManager;

    @Override
    public AuditLog create(String resourceType, Long resourceId, Map<String, Object> data, Identity identity,
                           Long accountId, Long authenticatedAsAccountId, String eventType, String authType, Long runTime,
                           String description, String clientIp) {
        AuditLog logs = create().newRecord(AuditLogTable.AUDIT_LOG);
        logs.setAccountId(accountId);
        logs.setAuthenticatedAsAccountId(authenticatedAsAccountId);
        logs.setEventType(eventType);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(DataUtils.FIELDS, data);
        logs.setData(dataMap);
        logs.setAuthenticatedAsIdentityId(identity != null ? identity.getId() : null);
        logs.setRuntime(runTime);
        logs.setDescription(description);
        logs.setAuthType(authType);
        logs.setResourceId(resourceId);
        logs.setResourceType(resourceType);
        logs.setClientIp(clientIp);
        objectManager.create(logs);
        return objectManager.reload(logs);
    }
}
