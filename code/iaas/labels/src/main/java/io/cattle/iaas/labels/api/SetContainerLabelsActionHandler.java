package io.cattle.iaas.labels.api;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SetContainerLabelsActionHandler implements ActionHandler {

    @Inject
    GenericMapDao mapDao;

    @Inject
    LabelsDao labelsDao;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    LabelsService labelsService;

    @Override
    public String getName() {
        return "instance.setlabels";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Instance instance = (Instance) obj;

        // are locks automatically applied for actions or just for process changes.  i'm guessing
        // just for process changes.
        //return lockManager.lock(new , callback)
        // TODO: wrap into DB transaction

        @SuppressWarnings("unchecked")
        Map<String, String> labelsFromInput = DataAccessor.fromMap(request.getRequestObject()).withKey(LabelConstants.INPUT_LABELS).as(Map.class);

        List<Label> existing = labelsDao.getLabelsForInstance(instance.getId());
        List<? extends InstanceLabelMap> existingInstanceMappings = mapDao.findNonRemoved(InstanceLabelMap.class, Instance.class, instance.getId());

        // figure out potentially new labels to create and associate with instance
        Map<String, String> existingLabels = new HashMap<String, String>();
        Map<Long, Label> existingLabelLookupById = new HashMap<Long, Label>();
        for (Label existingLabel : existing) {
            existingLabels.put(existingLabel.getKey(), existingLabel.getValue());
            existingLabelLookupById.put(existingLabel.getId(), existingLabel);
        }
        for (Map.Entry<String, String> inputEntry : labelsFromInput.entrySet()) {
            String existingValue = existingLabels.get(inputEntry.getKey());
            if (existingValue == null || !existingValue.equals(inputEntry.getValue())) {
                labelsService.createContainerLabel(
                        instance.getAccountId(), instance.getId(), inputEntry.getKey(), inputEntry.getValue());
            }
        }

        // figure out which mappings to remove
        for (InstanceLabelMap mapping : existingInstanceMappings) {
            Long labelId = mapping.getLabelId();
            Label existingLabel = existingLabelLookupById.get(labelId);
            String newLabelValue = labelsFromInput.get(existingLabel.getKey());
            if (newLabelValue == null || !newLabelValue.equals(existingLabel.getValue())) {
                processManager.scheduleStandardProcess(StandardProcess.REMOVE, mapping, null);
            }
        }

        return instance;
    }

}
