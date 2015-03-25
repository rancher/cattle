package io.cattle.platform.extension.dynamic.dao.impl;

import static io.cattle.platform.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP;
import static io.cattle.platform.core.model.tables.ExternalHandlerProcessTable.EXTERNAL_HANDLER_PROCESS;
import static io.cattle.platform.core.model.tables.ExternalHandlerTable.EXTERNAL_HANDLER;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.ExternalHandler;
import io.cattle.platform.core.model.tables.records.ExternalHandlerRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.extension.dynamic.dao.ExternalHandlerDao;
import io.cattle.platform.extension.dynamic.data.ExternalHandlerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.Result;

public class ExternalHandlerDaoImpl extends AbstractJooqDao implements ExternalHandlerDao {

    @Override
    public List<? extends ExternalHandler> getExternalHandler(String processName) {
        return create()
                .select(EXTERNAL_HANDLER.fields())
                .from(EXTERNAL_HANDLER)
                .join(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_ID.eq(EXTERNAL_HANDLER.ID))
                .join(EXTERNAL_HANDLER_PROCESS)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_PROCESS_ID.eq(EXTERNAL_HANDLER_PROCESS.ID))
                .where(
                        EXTERNAL_HANDLER_PROCESS.NAME.eq(processName)
                        .and(EXTERNAL_HANDLER.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_PROCESS.STATE.eq(CommonStatesConstants.ACTIVE)))
                .fetchInto(ExternalHandlerRecord.class);
    }
    
    public List<? extends ExternalHandlerData> getExternalHandlerData(String processName) {
    	List<Field<?>> fields = new ArrayList<Field<?>>(Arrays.asList(EXTERNAL_HANDLER.fields()));
    	fields.add(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.ON_ERROR);
    	 	
        return create()
                .select(fields)
                .from(EXTERNAL_HANDLER)
                .join(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_ID.eq(EXTERNAL_HANDLER.ID))
                .join(EXTERNAL_HANDLER_PROCESS)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_PROCESS_ID.eq(EXTERNAL_HANDLER_PROCESS.ID))
                .where(
                        EXTERNAL_HANDLER_PROCESS.NAME.eq(processName)
                        .and(EXTERNAL_HANDLER.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_PROCESS.STATE.eq(CommonStatesConstants.ACTIVE)))
                .fetchInto(ExternalHandlerData.class);
    }

}
