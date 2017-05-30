package io.cattle.platform.iaas.api.filter.registry;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class RegistryServerAddressFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> requestObject = CollectionUtils.toMap(request.getRequestObject());
        String serverAddress = (String) requestObject.get(StoragePoolConstants.SERVER_ADDRESS);
        long accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
        List<StoragePool> registries = objectManager.find(StoragePool.class,
                ObjectMetaDataManager.KIND_FIELD, StoragePoolConstants.KIND_REGISTRY,
                ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
                ObjectMetaDataManager.REMOVED_FIELD, null);
        for (StoragePool registry: registries){
            if (!CommonStatesConstants.REMOVED.equalsIgnoreCase(registry.getState())) {
                if (serverAddress.equalsIgnoreCase(
                        (String) CollectionUtils.getNestedValue(registry.getData(), "fields", StoragePoolConstants.SERVER_ADDRESS))) {
                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "ServerAddressUsed");
                }
            }
        }
        return super.create(type, request, next);
    }

    @Override
    public String[] getTypes() {
        return new String[] { StoragePoolConstants.KIND_REGISTRY };
    }
}
