package io.cattle.platform.systemstack.api;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class StackCreateFilter extends AbstractDefaultResourceManagerFilter {

    Logger log = LoggerFactory.getLogger(StackCreateFilter.class);

    @Inject
    CatalogService catalogService;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Stack.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Stack env = request.proxyRequestObject(Stack.class);

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
