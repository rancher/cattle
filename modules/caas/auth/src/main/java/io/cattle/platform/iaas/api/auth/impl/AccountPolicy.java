package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.impl.DefaultPolicy;
import io.cattle.platform.api.auth.impl.PolicyOptions;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AccountPolicy extends DefaultPolicy {

    private static final Logger log = LoggerFactory.getLogger(AccountPolicy.class);

    public AccountPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, PolicyOptions options) {
        super(account.getId(), authenticatedAsAccount.getId(), account.getName(), identities, options);
    }

    @Override
    public <T> T checkAuthorized(T obj) {
        if (hasGrantedAccess(obj)) {
            return obj;
        }

        if (isOption(AUTHORIZED_FOR_ALL_ACCOUNTS) || obj == null) {
            return obj;
        }

        if (obj instanceof Account) {
            return (((Account) obj).getId() == getAccountId()) ? obj : null;
        }

        Object isPublic = ObjectUtils.getPropertyIgnoreErrors(obj, ObjectMetaDataManager.PUBLIC_FIELD);
        if (isPublic instanceof Boolean && (Boolean) isPublic) {
            return obj;
        }

        try {
            Object prop = PropertyUtils.getProperty(obj, ObjectMetaDataManager.ACCOUNT_FIELD);
            if (prop != null && prop.equals(getAccountId())) {
                return obj;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to access [{}] field for authorization", ObjectMetaDataManager.ACCOUNT_FIELD, e);
        } catch (NoSuchMethodException e) {
            /* If it doesn't have "accountId," then its authorized */
            return obj;
        }

        log.error("Dropping unauthorized object [{}] for acccount [{}]", ObjectUtils.toStringWrapper(obj), getAccountId());
        return null;
    }

    @Override
    public Set<String> getRoles() {
        SchemaFactory sf = ApiContext.getSchemaFactory();
        return sf == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(sf.getId()));
    }

}
