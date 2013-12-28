package io.github.ibuildthecloud.dstack.api.object.postini;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.postinit.ObjectPostInstantiationHandler;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class AccountFieldPostInitHandler implements ObjectPostInstantiationHandler {

    public static final String ACCOUNT_ID = "accountId";

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        ApiContext apiContext = ApiContext.getContext();
        if ( apiContext == null ) {
            /* Backend can do whatever it wants */
            return obj;
        }

        Policy policy = ApiUtils.getPolicy();
        boolean overwrite = true;

        if ( policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && properties.containsKey(ACCOUNT_ID) ) {
            overwrite = false;
        }

        if ( overwrite ) {
            properties.put(ACCOUNT_ID, policy.getAccountId());
        }

        return obj;
    }

    protected void set(Object obj, String property, Object value) {
        try {
            BeanUtils.setProperty(obj, property, value);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
    }
}
