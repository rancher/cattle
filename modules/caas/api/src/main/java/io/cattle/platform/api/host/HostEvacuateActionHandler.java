package io.cattle.platform.api.host;

import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import static io.cattle.platform.core.model.tables.ExternalEventTable.*;

public class HostEvacuateActionHandler implements ActionHandler {

    GenericResourceDao resourceDao;

    public HostEvacuateActionHandler(GenericResourceDao resourceDao) {
        super();
        this.resourceDao = resourceDao;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (obj instanceof Host) {
            Host host = (Host) obj;
            resourceDao.createAndSchedule(ExternalEvent.class,
                    EXTERNAL_EVENT.KIND, ExternalEventConstants.KIND_EXTERNAL_HOST_EVENT,
                    EXTERNAL_EVENT.EVENT_TYPE, ExternalEventConstants.TYPE_HOST_EVACUATE,
                    ExternalEventConstants.FIELD_HOST_ID, host.getId(),
                    EXTERNAL_EVENT.ACCOUNT_ID, host.getAccountId());
            return host;
        } else {
            return null;
        }
    }
}