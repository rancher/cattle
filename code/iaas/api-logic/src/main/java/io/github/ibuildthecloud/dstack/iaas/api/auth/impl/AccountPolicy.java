package io.github.ibuildthecloud.dstack.iaas.api.auth.impl;

import io.github.ibuildthecloud.dstack.api.auth.impl.DefaultPolicy;
import io.github.ibuildthecloud.dstack.api.auth.impl.PolicyOptions;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountPolicy extends DefaultPolicy {

    private static final Logger log = LoggerFactory.getLogger(AccountPolicy.class);

    public AccountPolicy(Account account, PolicyOptions options) {
        super(account.getId(), account.getName(), Arrays.asList(account.getId()), options);
    }

    @Override
    public <T> T authorizeObject(T obj) {
        if ( isOption(AUTHORIZED_FOR_ALL_ACCOUNTS) || obj == null ) {
            return obj;
        } else {
            if ( obj instanceof Account ) {
                if ( ((Account)obj).getId().longValue() == getAccountId() ) {
                    return obj;
                } else {
                    return null;
                }
            }

            try {
                Object prop = PropertyUtils.getProperty(obj, ObjectMetaDataManager.ACCOUNT_FIELD);
                if ( prop != null && prop.equals(getAccountId()) ) {
                    return obj;
                } else {
                    Object isPublic = ObjectUtils.getPropertyIgnoreErrors(obj, ObjectMetaDataManager.PUBLIC_FIELD);
                    if ( isPublic instanceof Boolean && ((Boolean)isPublic).booleanValue() ) {
                        return obj;
                    }
                    log.error("Dropping unauthorized object [{}] for acccount [{}]", obj, getAccountId());
                }
            } catch (IllegalAccessException e) {
                log.error("Failed to access [{}] field for authorization", ObjectMetaDataManager.ACCOUNT_FIELD, e);
                return null;
            } catch (InvocationTargetException e) {
                log.error("Failed to access [{}] field for authorization", ObjectMetaDataManager.ACCOUNT_FIELD, e);
                return null;
            } catch (NoSuchMethodException e) {
                /* If it doesn't have "accountId," then its authorized */
                return obj;
            }
            return null;
        }
    }

}
