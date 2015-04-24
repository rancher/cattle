package io.cattle.platform.iaas.api.label;

import static io.cattle.platform.core.model.Tables.INSTANCE_LABEL_MAP;
import static io.cattle.platform.core.model.Tables.LABEL;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class AddContainerLabelActionHandler implements ActionHandler {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.addlabel";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Instance instance = (Instance) obj;

        String labelKey = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_KEY).as(String.class);
        String labelValue = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_VALUE).as(String.class);

        // TODO: Refactor to shared service with label creation/association in InstanceCreate
        // best effort for not duplicating; TODO: Handle this better
        Label label = objectManager.findAny(Label.class,
                LABEL.KEY, labelKey,
                LABEL.VALUE, labelValue,
                LABEL.ACCOUNT_ID, instance.getAccountId(),
                LABEL.REMOVED, null);
        if (label == null) {
            Map<Object, Object> labelData = new HashMap<>();
            labelData.put(LABEL.NAME, labelKey + "=" + labelValue);
            labelData.put(LABEL.KEY, labelKey);
            labelData.put(LABEL.VALUE, labelValue);
            labelData.put(LABEL.ACCOUNT_ID, instance.getAccountId());

            labelData.put(LABEL.TYPE, LabelConstants.HOST_TYPE);
            label = resourceDao.create(Label.class, objectManager.convertToPropertiesFor(Label.class, labelData));
        }

        // link label to instance

        // check link doesn't exist first.  TODO: Handle this better
        InstanceLabelMap mapping = objectManager.findAny(InstanceLabelMap.class,
                INSTANCE_LABEL_MAP.LABEL_ID, label.getId(),
                INSTANCE_LABEL_MAP.INSTANCE_ID, instance.getId(),
                INSTANCE_LABEL_MAP.ACCOUNT_ID, instance.getAccountId(),
                INSTANCE_LABEL_MAP.REMOVED, null);
        if (mapping == null) {
            Map<Object, Object> mappingData = new HashMap<>();
            mappingData.put(INSTANCE_LABEL_MAP.LABEL_ID, label.getId());
            mappingData.put(INSTANCE_LABEL_MAP.INSTANCE_ID, instance.getId());
            mappingData.put(INSTANCE_LABEL_MAP.ACCOUNT_ID, instance.getAccountId());
            resourceDao.createAndSchedule(InstanceLabelMap.class, objectManager.convertToPropertiesFor(InstanceLabelMap.class, mappingData));
        }

        return instance;
    }

}
