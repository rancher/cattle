package io.cattle.iaas.labels.api;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class AddHostLabelActionHandler implements ActionHandler {

    @Inject
    LabelsService labelsService;

    @Override
    public String getName() {
        return "host.addlabel";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Host host = (Host) obj;

        String labelKey = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_KEY).as(String.class);
        String labelValue = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_VALUE).as(String.class);

        labelsService.createHostLabel(host.getAccountId(), host.getId(), labelKey, labelValue);

        return host;
    }

}
