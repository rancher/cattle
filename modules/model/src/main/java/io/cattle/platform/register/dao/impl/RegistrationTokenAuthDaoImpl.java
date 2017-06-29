package io.cattle.platform.register.dao.impl;

import static io.cattle.platform.core.model.tables.CredentialTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.register.dao.RegistrationTokenAuthDao;

import org.jooq.Configuration;

public class RegistrationTokenAuthDaoImpl extends AbstractJooqDao implements RegistrationTokenAuthDao {

    public RegistrationTokenAuthDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Credential getCredential(String accessKey) {
        return create()
                .selectFrom(CREDENTIAL)
                .where(CREDENTIAL.KIND.eq(CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN)
                        .and(CREDENTIAL.PUBLIC_VALUE.eq(accessKey))
                        .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(CREDENTIAL.REMOVED.isNull()))
                .fetchOne();
    }

}
