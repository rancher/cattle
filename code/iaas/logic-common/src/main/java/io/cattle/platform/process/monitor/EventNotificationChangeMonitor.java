package io.cattle.platform.process.monitor;

import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Map;


public class EventNotificationChangeMonitor extends io.cattle.platform.engine.process.impl.EventNotificationChangeMonitor {

    @Override
    public void addData(Map<String, Object> data, boolean schedule, String previousState, String newState,
            ProcessRecord record, ProcessState state, ProcessServiceContext context) {
        Object resource = state.getResource();
        Object accountId = ObjectUtils.getAccountId(resource);

        if ( accountId != null ) {
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        }

        super.addData(data, schedule, previousState, newState, record, state, context);
    }

}
