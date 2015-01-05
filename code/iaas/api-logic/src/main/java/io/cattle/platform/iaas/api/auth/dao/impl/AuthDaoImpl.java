package io.cattle.platform.iaas.api.auth.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public class AuthDaoImpl extends AbstractJooqDao implements AuthDao {

    private DynamicStringListProperty SUPPORTED_TYPES = ArchaiusUtil.getList("account.by.key.credential.types");

    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    
    @Override
    public Account getAdminAccount() {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.STATE.eq("active")
                        .and(ACCOUNT.KIND.eq("admin"))
                ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

    @Override
    public Account getAccountByKeys(String access, String secretKey) {
        return create()
                .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                .join(CREDENTIAL)
                    .on(CREDENTIAL.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(
                    ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                    .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(CREDENTIAL.PUBLIC_VALUE.eq(access))
                    .and(CREDENTIAL.SECRET_VALUE.eq(secretKey)))
                    .and(CREDENTIAL.KIND.in(SUPPORTED_TYPES.get()))
                .fetchOneInto(AccountRecord.class);
    }

    @Override
    public Account getAccountByExternalId(String externalId, String externalType) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.EXTERNAL_ID.eq(externalId)
                        .and(ACCOUNT.EXTERNAL_ID_TYPE.eq(externalType))
                        .and(ACCOUNT.STATE.eq("active"))
                ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }
    
    @Override
    public Account createAccount(String name, String kind, String externalId, String externalType) {
        Map<String, Object> properties =  new HashMap<>();
        properties.put(ACCOUNT.NAME.toString(), name);
        properties.put(ACCOUNT.KIND.toString(), kind);
        properties.put(ACCOUNT.EXTERNAL_ID.toString(), externalId);
        properties.put(ACCOUNT.EXTERNAL_ID_TYPE.toString(), externalType);
        return resourceDao.createAndSchedule(Account.class, properties);
    }
    
    @Override
    public Account getAccountByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.UUID.eq(uuid)
                        .and(ACCOUNT.STATE.eq("active"))
                        ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }
    
    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }
    
    public GenericResourceDao getGenericResourceDao() {
        return resourceDao;
    }
    
    @Inject
    public void setGenericResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}
