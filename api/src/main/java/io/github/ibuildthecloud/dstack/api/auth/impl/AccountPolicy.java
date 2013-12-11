package io.github.ibuildthecloud.dstack.api.auth.impl;

import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountPolicy extends DefaultPolicy {

    private static final Logger log = LoggerFactory.getLogger(AccountPolicy.class);

    List<Long> authorized;
    Account account;

    public AccountPolicy(Account account) {
        super();
        this.account = account;
        this.authorized = Arrays.asList(account.getId());
    }

    @Override
    public List<Long> getAuthorizedAccounts() {
        return authorized;
    }

    @Override
    public <T> T authorize(T obj) {
        if ( isAuthorizedForAllAccounts() || obj == null ) {
            return obj;
        } else {
            try {
                Object prop = PropertyUtils.getProperty(obj, ObjectMetaDataManager.ACCOUNT_FIELD);
                if ( prop != null && prop.equals(account.getId()) ) {
                    return obj;
                } else {
                    log.error("Dropping unauthorized object [{}] for acccount [{}]", obj, account.getId());
                }
            } catch (IllegalAccessException e) {
                log.error("Failed to access [{}] field for authorization", ObjectMetaDataManager.ACCOUNT_FIELD, e);
                return null;
            } catch (InvocationTargetException e) {
                log.error("Failed to access [{}] field for authorization", ObjectMetaDataManager.ACCOUNT_FIELD, e);
                return null;
            } catch (NoSuchMethodException e) {
                return obj;
            }
            return null;
        }
    }

}
