package io.cattle.platform.iaas.api.auth.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.framework.encryption.EncryptionConstants;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.PasswordDao;
import io.cattle.platform.iaas.api.auth.integration.local.ChangePassword;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class PasswordDaoImpl extends AbstractJooqDao implements PasswordDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    TransformationService transformationService;

    @Override
    public Credential changePassword(Credential password, ChangePassword changePassword, Policy policy) {

        if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) || transformationService.compare(changePassword.getOldSecret(), password.getSecretValue())){
            password.setSecretValue(transformationService.transform(changePassword.getNewSecret(), EncryptionConstants.HASH));
            password = objectManager.persist(password);
            return password;
        }

        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidOldPassword");
    }

    @Override
    public Credential updateAdminAndCredentials(String username, String password, String name) {
        Account admin = create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(ACCOUNT.KIND.eq(AccountConstants.ADMIN_KIND)))
                .orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();

        if (admin == null){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "UnableToFindAdmin");
        }

         if (create().selectFrom(CREDENTIAL)
                .where(CREDENTIAL.PUBLIC_VALUE.eq(username)
                        .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                                .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_PASSWORD)
                                .and(CREDENTIAL.ACCOUNT_ID.ne(admin.getId())))))
                .fetchAny() != null){
             throw new ClientVisibleException(ResponseCodes.CONFLICT, "UsernameIsTaken", "Username: " + username +" is taken", null);
         }

        create().delete(CREDENTIAL)
                .where(CREDENTIAL.ACCOUNT_ID.eq(admin.getId())
                        .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_PASSWORD)))
                .execute();

        Map<Object, Object> properties = new HashMap<>();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "UserCredentialCreation", "Cannot have blank username or password.", null);
        }

        properties.put(CREDENTIAL.PUBLIC_VALUE, username);
        properties.put(CREDENTIAL.SECRET_VALUE, transformationService.transform(password, EncryptionConstants.HASH));
        properties.put(CREDENTIAL.KIND, LocalAuthConstants.PASSWORD);
        properties.put(CREDENTIAL.ACCOUNT_ID, admin.getId());

        DataAccessor.fields(admin).withKey(SecurityConstants.HAS_LOGGED_IN).set(true);
        admin.setName(name);
        objectManager.persist(admin);

        return resourceDao.createAndSchedule(Credential.class, objectManager.convertToPropertiesFor(Credential.class,
                properties));
    }

    @Override
    public boolean isUnique(final Credential credential) {

        if (StringUtils.isNotBlank(credential.getPublicValue())
                && CredentialConstants.CREDENTIAL_TYPES_TO_FILTER.contains(credential.getKind())) {
                    List<Credential> credentials = create().selectFrom(CREDENTIAL)
                            .where(CREDENTIAL.REMOVED.isNull()
                                    .and(CREDENTIAL.PUBLIC_VALUE.equalIgnoreCase(credential.getPublicValue())
                                            .and(CREDENTIAL.KIND.equalIgnoreCase(credential.getKind())))).fetchInto
                                    (Credential.class);
                    return credentials.isEmpty();
        }
        return true;
    }

}
