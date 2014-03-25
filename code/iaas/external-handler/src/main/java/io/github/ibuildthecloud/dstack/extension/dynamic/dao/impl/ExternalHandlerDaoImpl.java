package io.github.ibuildthecloud.dstack.extension.dynamic.dao.impl;

import java.util.List;

import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.ExternalHandler;
import io.github.ibuildthecloud.dstack.core.model.tables.records.ExternalHandlerRecord;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.extension.dynamic.dao.ExternalHandlerDao;
import static io.github.ibuildthecloud.dstack.core.model.tables.ExternalHandlerProcessTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ExternalHandlerTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.*;

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

}
