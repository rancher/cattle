package io.cattle.iaas.labels.api;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class AddContainerLabelActionHandler implements ActionHandler {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    LabelsService labelsService;

    @Override
    public String getName() {
        return "instance.addlabel";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Instance instance = (Instance) obj;

        String labelKey = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_KEY).as(String.class);
        String labelValue = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_VALUE).as(String.class);

        labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), labelKey, labelValue);

        return instance;
    }

}
