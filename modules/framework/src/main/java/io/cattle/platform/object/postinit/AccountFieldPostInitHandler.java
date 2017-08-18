package io.cattle.platform.object.postinit;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import java.util.Map;

public class AccountFieldPostInitHandler implements ObjectPostInstantiationHandler {

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        ApiContext apiContext = ApiContext.getContext();
        if (apiContext == null) {
            /* Back-end can do whatever it wants */
            return obj;
        }

        Policy policy = ApiUtils.getPolicy();
        boolean overwrite = true;

        if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && properties.containsKey(ObjectMetaDataManager.ACCOUNT_FIELD)) {
            overwrite = false;
        }

        if (overwrite) {
            Long accountId = policy.getAccountId();
            String value = policy.getOption(Policy.RESOURCE_ACCOUNT_ID);
            if (value != null) {
                accountId = Long.parseLong(value);
            } else if (policy.isOption(Policy.OVERRIDE_ACCOUNT_ID)) {
                accountId = null;
            }
            ObjectUtils.setPropertyIgnoreErrors(obj, ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
            if (!properties.containsKey(ObjectMetaDataManager.CLUSTER_FIELD)) {
                ObjectUtils.setPropertyIgnoreErrors(obj, ObjectMetaDataManager.CLUSTER_FIELD, policy.getClusterId());
            }
        }

        ObjectUtils.setPropertyIgnoreErrors(obj, ObjectMetaDataManager.CREATOR_FIELD, policy.getAuthenticatedAsAccountId());
        return obj;
    }

}
