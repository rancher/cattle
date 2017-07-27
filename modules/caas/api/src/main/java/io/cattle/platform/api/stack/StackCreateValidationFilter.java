package io.cattle.platform.api.stack;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackCreateValidationFilter extends AbstractValidationFilter {

    private static final Logger log = LoggerFactory.getLogger(StackCreateValidationFilter.class);

    CatalogService catalogService;
    ResourceManagerLocator locator;
    ObjectManager objMgr;

    public StackCreateValidationFilter(ResourceManagerLocator locator, ObjectManager objMgr, CatalogService catalogService) {
        super();
        this.locator = locator;
        this.objMgr = objMgr;
        this.catalogService = catalogService;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Stack env = request.proxyRequestObject(Stack.class);

        if (env.getName().startsWith("-") || env.getName().endsWith("-")) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }

        ResourceManager rm = locator.getResourceManagerByType(type);

        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(ObjectMetaDataManager.NAME_FIELD, env.getName());
        criteria.put(ObjectMetaDataManager.REMOVED_FIELD, new Condition(ConditionType.NULL));
        List<?> existingEnv = rm.list(type, criteria, null);
        if (!existingEnv.isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "name");
        }

        if (catalogService.isEnabled()) {
            String externalId = env.getExternalId();
            try {
                String templateBase = catalogService.getTemplateBase(externalId);
                if ("kubernetes".equals(templateBase)) {
                    env.setKind("kubernetesStack");
                }
            } catch (IOException e) {
                log.error("Failed to contact catalog", e);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "Failed to contact catalog");
            }
        }

        return super.create(type, request, next);
    }

}
