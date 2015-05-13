package io.cattle.platform.iaas.api.label;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import static io.cattle.platform.core.model.Tables.HOST_LABEL_MAP;
import static io.cattle.platform.core.model.Tables.LABEL;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class AddHostLabelActionHandler implements ActionHandler {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "host.addlabel";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Host host = (Host) obj;

        String labelKey = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_KEY).as(String.class);
        String labelValue = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABEL_VALUE).as(String.class);

        // best effort for not duplicating; TODO: Handle this better
        Label label = objectManager.findAny(Label.class,
                LABEL.KEY, labelKey,
                LABEL.VALUE, labelValue,
                LABEL.ACCOUNT_ID, host.getAccountId(),
                LABEL.REMOVED, null);
        if (label == null) {
            Map<Object, Object> labelData = new HashMap<>();
            labelData.put(LABEL.NAME, labelKey + "=" + labelValue);
            labelData.put(LABEL.KEY, labelKey);
            labelData.put(LABEL.VALUE, labelValue);
            labelData.put(LABEL.ACCOUNT_ID, host.getAccountId());

            labelData.put(LABEL.TYPE, LabelConstants.HOST_TYPE);
            label = resourceDao.create(Label.class, objectManager.convertToPropertiesFor(Label.class, labelData));
        }

        // link label to host

        // check link doesn't exist first.  TODO: Handle this better
        HostLabelMap mapping = objectManager.findAny(HostLabelMap.class,
                HOST_LABEL_MAP.LABEL_ID, label.getId(),
                HOST_LABEL_MAP.HOST_ID, host.getId(),
                HOST_LABEL_MAP.REMOVED, null);
        if (mapping == null) {
            Map<Object, Object> mappingData = new HashMap<>();
            mappingData.put(HOST_LABEL_MAP.LABEL_ID, label.getId());
            mappingData.put(HOST_LABEL_MAP.HOST_ID, host.getId());
            resourceDao.createAndSchedule(HostLabelMap.class, objectManager.convertToPropertiesFor(HostLabelMap.class, mappingData));
        }

        return host;
    }

}
