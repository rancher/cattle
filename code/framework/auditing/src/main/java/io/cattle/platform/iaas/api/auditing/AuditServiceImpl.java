package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ContainerEventConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.iaas.api.auditing.dao.AuditLogDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditServiceImpl implements AuditService{

    public static final Set<String> BLACK_LIST_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "publish",
                    "configContent".toLowerCase(),
                    "externalHandler".toLowerCase(),
                    "externalService".toLowerCase(),
                    "hostApiProxyToken".toLowerCase(),
                    "token",
                    "scripts",
                    "serviceEvent".toLowerCase(),
                    "userPreference".toLowerCase(),
                    "dynamicSchema".toLowerCase(),
                    ContainerEventConstants.CONTAINER_EVENT_KIND.toLowerCase(),
                    ExternalEventConstants.KIND_EXTERNAL_EVENT.toLowerCase(),
                    ExternalEventConstants.KIND_SERVICE_EVENT.toLowerCase(),
                    ExternalEventConstants.KIND_VOLUME_EVENT.toLowerCase(),
                    ExternalEventConstants.KIND_STORAGE_POOL_EVENT.toLowerCase()
            )));

    @Inject
    AuditLogDao auditLogDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    EventService eventService;

    @Inject
    ObjectManager objectManager;

    @Inject
    IdFormatter idFormatter;

    private static final Logger log = LoggerFactory.getLogger(AuditLogsRequestHandler.class);

    @Override
    public void logRequest(ApiRequest request, Policy policy) {
        if (policy == null || request == null || request.getType() == null) {
            return;
        }

        if (Schema.Method.GET.isMethod(request.getMethod()) ||
                BLACK_LIST_TYPES.contains(request.getType().toLowerCase())) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        putInAsString(data, "requestObject", "Failed to convert request object to json.", request.getRequestObject());
        putInAsString(data, "responseObject", "Failed to convert response object to json.", request.getResponseObject());
        data.put("responseCode", request.getResponseCode());
        Identity user = null;
        for (Identity identity: policy.getIdentities()){
            if (identity.getExternalIdType().contains("user")){
                user = identity;
                break;
            }
        }
        if (user == null && policy.getIdentities().size() == 1){
            user = policy.getIdentities().iterator().next();
        }
        long runtime = ((long) request.getAttribute("requestEndTime")) - ((long)request.getAttribute("requestStartTime"));
        String authType = (String) request.getAttribute(AccountConstants.AUTH_TYPE);
        String resourceId =request.getResponseObject() instanceof Resource ? ((Resource) request.getResponseObject()).getId(): null;
        String resourceType = request.getType();
        String eventType = "api." + convertResourceType(resourceType) + "." + (StringUtils.isNotBlank(request.getAction()) ?
                request.getAction() :convertToAction(request.getMethod()));
        auditLogDao.create(resourceType, parseId(resourceId), data, user,
                policy.getAccountId(), policy.getAuthenticatedAsAccountId(), eventType, authType, runtime, null,
                request.getClientIp());
    }

    private String convertResourceType(String type) {
        switch (StringUtils.lowerCase(type)) {
            case "environment":
                return "stack";
            case "project":
                return "environment";
            default:
                return type;
        }
    }

    private Long parseId(String resourceId) {
        Long parsedResourceId;
        if (resourceId == null){
            parsedResourceId = null;
        } else try {
            if (ApiContext.getContext() != null && ApiContext.getContext().getIdFormatter() != null) {
                parsedResourceId = Long.valueOf(ApiContext.getContext().getIdFormatter().parseId(resourceId));
            } else {
                parsedResourceId = Long.valueOf(idFormatter.parseId(resourceId));
            }
        } catch (NumberFormatException e) {
            try {
                parsedResourceId = Long.valueOf(resourceId);
            } catch (NumberFormatException e1) {
                parsedResourceId = null;
            }
        }
        return parsedResourceId;
    }

    private AuditEventType convertToAction(String method) {
        switch (Schema.Method.valueOf(method)){
            case DELETE:
                return AuditEventType.delete;
            case POST:
                return AuditEventType.create;
            case PUT:
                return AuditEventType.update;
            default:
                return AuditEventType.UNKNOWN;
        }
    }

    private void putInAsString(Map<String, Object> data, String fieldForObject, String errMsg, Object objectToPlace) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            jsonMapper.writeValue(os, objectToPlace);
            data.put(fieldForObject, os.toString());
        } catch (IOException e) {
            log.error("Failed to log [{}]", errMsg, e);
        }
    }
}
