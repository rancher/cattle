package io.github.ibuildthecloud.dstack.iaas.api.object.postinit;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.core.constants.AccountConstants;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.postinit.ObjectPostInstantiationHandler;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import java.util.Map;

public class AccountFieldPostInitHandler implements ObjectPostInstantiationHandler {

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        ApiContext apiContext = ApiContext.getContext();
        if ( apiContext == null ) {
            /* Back-end can do whatever it wants */
            return obj;
        }

        Policy policy = ApiUtils.getPolicy();
        boolean overwrite = true;

        if ( policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && properties.containsKey(AccountConstants.ACCOUNT_ID) ) {
            overwrite = false;
        }

        if ( overwrite ) {
            ObjectUtils.setPropertyIgnoreErrors(obj, ObjectMetaDataManager.ACCOUNT_FIELD, policy.getAccountId());
        }

        return obj;
    }

}
