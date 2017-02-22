package io.cattle.platform.iaas.api.host;

import static io.cattle.platform.core.model.tables.ExternalEventTable.*;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class HostEvacuateActionHandler implements ActionHandler {

    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
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

    @Override
    public String getName() {
        return "host.evacuate";
    }
}