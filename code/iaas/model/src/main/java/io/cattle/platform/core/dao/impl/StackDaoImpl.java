package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Record2;
import org.jooq.RecordHandler;

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

        @Override
        public Map<Long, List<Object>> getServicesForStack(List<Long> ids, final IdFormatter idFormatter) {
            final Map<Long, List<Object>> result = new HashMap<>();
            create().select(SERVICE.ID, SERVICE.STACK_ID)
                .from(SERVICE)
                .where(SERVICE.STACK_ID.in(ids)
                        .and(SERVICE.REMOVED.isNull()))
                .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                    @Override
                    public void next(Record2<Long, Long> record) {
                        Long id = record.getValue(SERVICE.ID);
                        Long stackId = record.getValue(SERVICE.STACK_ID);
                        List<Object> list = result.get(stackId);
                        if (list == null) {
                            list = new ArrayList<>();
                            result.put(stackId, list);
                        }
                        list.add(idFormatter.formatId(ServiceConstants.KIND_SERVICE, id));
                    }
                });
            return result;
        }
}
