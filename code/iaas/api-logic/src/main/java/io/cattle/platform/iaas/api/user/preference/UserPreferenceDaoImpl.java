package io.cattle.platform.iaas.api.user.preference;

import io.cattle.platform.core.model.UserPreference;
import static io.cattle.platform.core.model.tables.UserPreferenceTable.*;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class UserPreferenceDaoImpl extends AbstractJooqDao implements UserPreferenceDao {
    @Override
    public boolean isUnique(UserPreference userPreference, long accountId) {
        return create().selectFrom(USER_PREFERENCE)
                .where(USER_PREFERENCE.NAME.eq(userPreference.getName())
                        .and(USER_PREFERENCE.ACCOUNT_ID.eq(accountId)).and(USER_PREFERENCE.REMOVED.isNull())).fetch().size() == 0;
    }
}
