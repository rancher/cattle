package io.cattle.iaas.labels.service.impl;

import static io.cattle.platform.core.model.Tables.HOST_LABEL_MAP;
import static io.cattle.platform.core.model.Tables.INSTANCE_LABEL_MAP;
import static io.cattle.platform.core.model.Tables.LABEL;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.core.model.InstanceLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LabelsServiceImpl implements LabelsService {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public Label getOrCreateLabel(Long accountId, String key, String value,
            String type) {
        // best effort for not duplicating; TODO: Handle this better
        Label label = objectManager.findAny(Label.class,
                LABEL.KEY, key,
                LABEL.VALUE, value,
                LABEL.ACCOUNT_ID, accountId,
                LABEL.REMOVED, null);
        if (label == null) {
            Map<Object, Object> labelData = new HashMap<>();
            labelData.put(LABEL.NAME, key + "=" + value);
            labelData.put(LABEL.KEY, key);
            labelData.put(LABEL.VALUE, value);
            labelData.put(LABEL.ACCOUNT_ID, accountId);

            labelData.put(LABEL.TYPE, type);
            label = resourceDao.create(Label.class, objectManager.convertToPropertiesFor(Label.class, labelData));
        }
        return label;
    }

    @Override
    public void createContainerLabel(Long accountId, Long instanceId,
            String key, String value) {
        Label label = getOrCreateLabel(accountId, key, value, LabelConstants.CONTAINER_TYPE);

        // check link doesn't exist first.  TODO: Handle this better
        InstanceLabelMap mapping = objectManager.findAny(InstanceLabelMap.class,
                INSTANCE_LABEL_MAP.LABEL_ID, label.getId(),
                INSTANCE_LABEL_MAP.INSTANCE_ID, instanceId,
                INSTANCE_LABEL_MAP.ACCOUNT_ID, accountId,
                INSTANCE_LABEL_MAP.REMOVED, null);
        if (mapping == null) {
            Map<Object, Object> mappingData = new HashMap<>();
            mappingData.put(INSTANCE_LABEL_MAP.LABEL_ID, label.getId());
            mappingData.put(INSTANCE_LABEL_MAP.INSTANCE_ID, instanceId);
            mappingData.put(INSTANCE_LABEL_MAP.ACCOUNT_ID, accountId);
            resourceDao.createAndSchedule(
                    InstanceLabelMap.class,
                    objectManager.convertToPropertiesFor(InstanceLabelMap.class, mappingData));
        }
    }

    @Override
    public void createHostLabel(Long accountId, Long hostId, String key,
            String value) {
        Label label = getOrCreateLabel(accountId, key, value, LabelConstants.HOST_TYPE);

        // link label to host

        // check link doesn't exist first.  TODO: Handle this better
        HostLabelMap mapping = objectManager.findAny(HostLabelMap.class,
                HOST_LABEL_MAP.LABEL_ID, label.getId(),
                HOST_LABEL_MAP.HOST_ID, hostId,
                HOST_LABEL_MAP.REMOVED, null);
        if (mapping == null) {
            Map<Object, Object> mappingData = new HashMap<>();
            mappingData.put(HOST_LABEL_MAP.LABEL_ID, label.getId());
            mappingData.put(HOST_LABEL_MAP.HOST_ID, hostId);
            mappingData.put(HOST_LABEL_MAP.ACCOUNT_ID, accountId);
            resourceDao.createAndSchedule(HostLabelMap.class, objectManager.convertToPropertiesFor(HostLabelMap.class, mappingData));
        }
    }

}
