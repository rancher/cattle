package io.cattle.platform.iaas.api.label;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class RemoveHostLabelActionHandler implements ActionHandler {

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectProcessManager processManager;

    @Override
    public String getName() {
        return "host.removelabel";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Host host = (Host) obj;

        Long labelId = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL).as(Long.class);
        // for now, just remove the mapping.  I'm wondering whether we should have a separate process that periodically
        // does cleanup
        HostLabelMap mapping = mapDao.findToRemove(HostLabelMap.class, Host.class, host.getId(), Label.class, labelId);
        if (mapping != null) {
            processManager.scheduleProcessInstance("hostlabelmap.remove", mapping, null);
        }
        return host;
    }

}
