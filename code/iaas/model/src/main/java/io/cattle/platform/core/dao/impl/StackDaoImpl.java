package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StackTable.STACK;

import javax.inject.Inject;

import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

public class StackDaoImpl extends AbstractJooqDao implements StackDao {
	
	 @Inject
	    ObjectManager objectManager;

	    @Override
	    public Stack getStackByExternalId(Long accountId, String externalId) {
	        return create().selectFrom(STACK)
	                .where(STACK.ACCOUNT_ID.eq(accountId))
	                .and(STACK.REMOVED.isNull())
	                .and(STACK.EXTERNAL_ID.eq(externalId))
	                .fetchAny();
	    }
}
