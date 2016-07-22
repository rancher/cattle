package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;

import javax.inject.Inject;

import io.cattle.platform.core.dao.EnvironmentDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

public class EnvironmentDaoImpl extends AbstractJooqDao implements EnvironmentDao {
	
	 @Inject
	    ObjectManager objectManager;

	    @Override
	    public Environment getEnvironmentByExternalId(Long accountId, String externalId) {
	        return create().selectFrom(ENVIRONMENT)
	                .where(ENVIRONMENT.ACCOUNT_ID.eq(accountId))
	                .and(ENVIRONMENT.REMOVED.isNull())
	                .and(ENVIRONMENT.EXTERNAL_ID.eq(externalId))
	                .fetchAny();
	    }
}
