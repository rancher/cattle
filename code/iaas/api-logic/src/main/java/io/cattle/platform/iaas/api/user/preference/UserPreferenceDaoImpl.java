package io.cattle.platform.iaas.api.user.preference;

import static io.cattle.platform.core.model.tables.UserPreferenceTable.*;

import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import org.jooq.Configuration;

public class UserPreferenceDaoImpl extends AbstractJooqDao implements UserPreferenceDao {

    public UserPreferenceDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public boolean isUnique(UserPreference userPreference, long accountId) {
        return create().selectFrom(USER_PREFERENCE)
                .where(USER_PREFERENCE.NAME.eq(userPreference.getName())
                        .and(USER_PREFERENCE.ACCOUNT_ID.eq(accountId)).and(USER_PREFERENCE.REMOVED.isNull())).fetch().size() == 0;
    }
}
